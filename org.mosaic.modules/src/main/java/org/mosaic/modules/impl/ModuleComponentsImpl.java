package org.mosaic.modules.impl;

import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.mosaic.modules.*;
import org.mosaic.modules.spi.ModuleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterators.toArray;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ModuleComponentsImpl extends Lifecycle implements ModuleComponents
{
    private static final Logger COMPONENTS_LOG = LoggerFactory.getLogger( ModuleComponentsImpl.class.getName() + ".components" );

    private static final Logger CLASS_LOAD_ERRORS_LOG = LoggerFactory.getLogger( ModuleComponentsImpl.class.getName() + ".classloading" );

    public static class ComponentDependency
    {
    }

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private LoadingCache<Class<?>, ComponentDescriptorImpl<?>> componentLookupCache;

    @Nonnull
    private Map<Class<?>, ComponentDescriptorImpl<?>> componentDescriptors = Collections.emptyMap();

    @Nullable
    private String moduleActivatorClassName;

    @Nullable
    private ModuleActivator activator;

    ModuleComponentsImpl( @Nonnull ModuleImpl module )
    {
        this.module = module;
        this.componentLookupCache = createComponentTypeCache( Collections.<Class<?>, ComponentDescriptorImpl<?>>emptyMap() );
    }

    @Override
    public String toString()
    {
        return "ModuleComponents[" + this.module + "]";
    }

    @Nonnull
    @Override
    public Class<?> loadClass( @Nonnull String className ) throws ClassNotFoundException
    {
        if( this.module.isActivated() )
        {
            return this.module.getBundle().loadClass( className );
        }
        throw new IllegalStateException( "module " + this.module + " is not active" );
    }

    @Nonnull
    @Override
    public <T> T getComponent( @Nonnull Class<T> type )
    {
        if( Module.class.equals( type ) )
        {
            return type.cast( this.module );
        }

        ComponentDescriptor<T> componentDescriptor = ( ComponentDescriptor<T> ) this.componentLookupCache.getUnchecked( type );
        return type.cast( componentDescriptor.getInstance() );
    }

    @Nullable
    @Override
    public <T> ComponentDescriptor<T> getComponentDescriptor( @Nonnull Class<T> type )
    {
        return ( ComponentDescriptor<T> ) this.componentDescriptors.get( type );
    }

    @Nonnull
    ModuleImpl getModule()
    {
        return this.module;
    }

    @Override
    protected synchronized void onBeforeStart()
    {
        if( this.module.isInternal() )
        {
            // not managing the internal bundles, waist of time
            return;
        }

        // create a map of all components in the module
        // then, create a cache of component descriptors for component type (including non-concrete keys)
        // then, create a directed graph mapping dependencies between loaded components
        Map<Class<?>, ComponentDescriptorImpl<?>> componentDescriptors = createComponentsMap();
        LoadingCache<Class<?>, ComponentDescriptorImpl<?>> componentDescriptorsCache = createComponentTypeCache( componentDescriptors );
        SimpleDirectedGraph<ComponentDescriptorImpl<?>, ComponentDependency> componentsGraph = createComponentsGraph( componentDescriptors, componentDescriptorsCache );

        // find the module activator, if any
        String moduleActivatorClassName = this.module.getBundle().getHeaders().get( "Module-Activator" );

        // clear old components
        clearChildren();

        // iterate descriptors, starting from dependent descriptors, then the requiring descriptors
        // ensures that if A uses B, we first create/activate B and only then A
        List<Lifecycle> children = asList( toArray( new TopologicalOrderIterator<>( componentsGraph ), Lifecycle.class ) );
        if( !children.isEmpty() )
        {
            // we want dependANT first (eg. a depends on b, then we should add b and then a)
            Collections.reverse( children );

            COMPONENTS_LOG.debug( "Components for {}:\n    -> {}", this.module, Joiner.on( "\n    -> " ).join( children ) );
            for( Lifecycle child : children )
            {
                addChild( child );
            }
        }

        // save
        this.componentDescriptors = componentDescriptors;
        this.componentLookupCache = componentDescriptorsCache;
        this.moduleActivatorClassName = moduleActivatorClassName;
    }

    @Override
    protected synchronized void onBeforeActivate()
    {
        if( this.moduleActivatorClassName != null )
        {
            Class<?> clazz;
            try
            {
                clazz = this.module.getBundle().loadClass( this.moduleActivatorClassName );
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not load activator class '" + this.moduleActivatorClassName + "' for module '" + this.module + "': " + e.getMessage(), e );
            }

            try
            {
                Class<? extends ModuleActivator> activatorClass = clazz.asSubclass( ModuleActivator.class );
                Constructor<? extends ModuleActivator> ctor = activatorClass.getDeclaredConstructor();
                ctor.setAccessible( true );
                this.activator = ctor.newInstance();
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not create activator '" + this.moduleActivatorClassName + "' for module '" + this.module + "': " + e.getMessage(), e );
            }

            this.activator.onBeforeActivate( this.module );
        }

        super.onBeforeActivate();
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        super.onAfterDeactivate();

        if( this.activator != null )
        {
            try
            {
                this.activator.onAfterDeactivate( this.module );
            }
            catch( Throwable e )
            {
                COMPONENTS_LOG.error( "Module activator '{}' threw exception: {}", this.activator, e.getMessage(), e );
            }
            finally
            {
                this.activator = null;
            }
        }
    }

    @Override
    protected synchronized void onAfterStop()
    {
        this.componentDescriptors = Collections.emptyMap();
        this.componentLookupCache = createComponentTypeCache( Collections.<Class<?>, ComponentDescriptorImpl<?>>emptyMap() );
    }

    @Nonnull
    private Map<Class<?>, ComponentDescriptorImpl<?>> createComponentsMap()
    {
        Map<Class<?>, ComponentDescriptorImpl<?>> componentDescriptors = new ConcurrentHashMap<>();
        for( URL resource : this.module.getModuleResources().findResources( "**/*.class" ) )
        {
            String className = resource.getPath().replace( "/", "." );
            className = className.substring( 0, className.length() - ".class".length() );
            if( className.startsWith( "." ) )
            {
                className = className.substring( 1 );
            }

            Class<?> clazz;
            try
            {
                clazz = this.module.getBundle().loadClass( className );
                ComponentDescriptorImpl<?> componentDescriptor = new ComponentDescriptorImpl<>( this.module, clazz );
                if( !componentDescriptor.isPlain() )
                {
                    componentDescriptors.put( clazz, componentDescriptor );
                }
            }
            catch( ClassNotFoundException | NoClassDefFoundError e )
            {
                CLASS_LOAD_ERRORS_LOG.trace( "Could not load class '{}' from module {}: {}", className, this.module, e.getMessage(), e );
            }
        }
        return componentDescriptors;
    }

    @Nonnull
    private LoadingCache<Class<?>, ComponentDescriptorImpl<?>> createComponentTypeCache( final Map<Class<?>, ComponentDescriptorImpl<?>> componentDescriptors )
    {
        return CacheBuilder
                .newBuilder()
                .concurrencyLevel( 5 )
                .initialCapacity( 100 )
                .build( new CacheLoader<Class<?>, ComponentDescriptorImpl<?>>()
                {
                    @Nonnull
                    @Override
                    public ComponentDescriptorImpl<?> load( @Nonnull Class<?> type ) throws Exception
                    {
                        List<ComponentDescriptorImpl<?>> candidates = null;
                        for( ComponentDescriptorImpl<?> descriptor : componentDescriptors.values() )
                        {
                            if( type.isAssignableFrom( descriptor.getComponentType() ) )
                            {
                                if( candidates == null )
                                {
                                    candidates = new LinkedList<>();
                                }
                                candidates.add( descriptor );
                            }
                        }

                        if( candidates == null || candidates.isEmpty() )
                        {
                            throw new ComponentNotFoundException( type, ModuleComponentsImpl.this.module );
                        }
                        else if( candidates.size() > 1 )
                        {
                            throw new TooManyComponentsFoundException( type, ModuleComponentsImpl.this.module );
                        }
                        else
                        {
                            return candidates.get( 0 );
                        }
                    }
                } );
    }

    @Nonnull
    private SimpleDirectedGraph<ComponentDescriptorImpl<?>, ComponentDependency> createComponentsGraph(
            @Nonnull Map<Class<?>, ComponentDescriptorImpl<?>> componentDescriptors,
            @Nonnull LoadingCache<Class<?>, ComponentDescriptorImpl<?>> componentDescriptorsCache )
    {
        SimpleDirectedGraph<ComponentDescriptorImpl<?>, ComponentDependency> componentsGraph = new SimpleDirectedGraph<>( ComponentDependency.class );
        for( ComponentDescriptorImpl<?> componentDescriptor : componentDescriptors.values() )
        {
            componentsGraph.addVertex( componentDescriptor );

            // a component by definition requires all its superclasses as components too
            Class<?> type = componentDescriptor.getComponentType().getSuperclass();
            while( type != null )
            {
                ComponentDescriptorImpl<?> superClassComponent = componentDescriptors.get( type );
                if( superClassComponent != null )
                {
                    componentsGraph.addVertex( superClassComponent );
                    componentsGraph.addEdge( componentDescriptor, superClassComponent );
                }
                type = type.getSuperclass();
            }

            // a component requires wired field components too
            for( ComponentFieldComponentLifecycle componentField : componentDescriptor.getChildren( ComponentFieldComponentLifecycle.class, false ) )
            {
                Class<?> fieldComponentType = componentField.getField().getType();
                if( !Module.class.equals( fieldComponentType ) )
                {
                    ComponentDescriptorImpl<?> requiredComponentDescriptor = componentDescriptorsCache.getUnchecked( fieldComponentType );
                    if( requiredComponentDescriptor.equals( componentDescriptor ) )
                    {
                        String msg = "component " + componentDescriptor + " requires itself";
                        throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), this.module );
                    }
                    else
                    {
                        componentsGraph.addVertex( requiredComponentDescriptor );
                        componentsGraph.addEdge( componentDescriptor, requiredComponentDescriptor );
                    }
                }
            }
        }
        return componentsGraph;
    }
}
