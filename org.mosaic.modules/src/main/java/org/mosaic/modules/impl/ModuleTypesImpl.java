package org.mosaic.modules.impl;

import com.google.common.base.Joiner;
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
import org.mosaic.modules.ComponentDefinitionException;
import org.mosaic.modules.ModuleTypes;
import org.mosaic.modules.spi.ModuleActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterators.toArray;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
@SuppressWarnings( "unchecked" )
final class ModuleTypesImpl extends Lifecycle implements ModuleTypes
{
    private static final Logger TYPES_LOG = LoggerFactory.getLogger( ModuleTypesImpl.class.getName() + ".components" );

    private static final Logger CLASS_LOAD_ERRORS_LOG = LoggerFactory.getLogger( ModuleTypesImpl.class.getName() + ".classloading" );

    public static class ComponentDependency
    {
    }

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private Map<Class<?>, TypeDescriptor> types = Collections.emptyMap();

    @Nullable
    private String moduleActivatorClassName;

    @Nullable
    private ModuleActivator activator;

    ModuleTypesImpl( @Nonnull ModuleImpl module )
    {
        this.module = module;
    }

    @Override
    public String toString()
    {
        return "ModuleTypes[" + this.module + "]";
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

    @Nullable
    @Override
    public org.mosaic.modules.TypeDescriptor getTypeDescriptor( @Nonnull Class<?> type )
    {
        return this.types.get( type );
    }

    @Nonnull
    ModuleImpl getModule()
    {
        return this.module;
    }

    @Nonnull
    List<TypeDescriptor> getTypeDescriptors( @Nonnull Class<?> type )
    {
        // TODO: cache result (clear cache on restart)

        List<TypeDescriptor> typeDescriptors = null;
        for( TypeDescriptor typeDescriptor : this.types.values() )
        {
            if( type.isAssignableFrom( typeDescriptor.getType() ) )
            {
                if( typeDescriptors == null )
                {
                    typeDescriptors = new LinkedList<>();
                }
                typeDescriptors.add( typeDescriptor );
            }
        }
        return typeDescriptors == null ? Collections.<TypeDescriptor>emptyList() : typeDescriptors;
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
        Map<Class<?>, TypeDescriptor> types = createComponentsMap();
        SimpleDirectedGraph<TypeDescriptor, ComponentDependency> componentsGraph = createComponentsGraph( types );

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

            TYPES_LOG.debug( "Components for {}:\n    -> {}", this.module, Joiner.on( "\n    -> " ).join( children ) );
            for( Lifecycle child : children )
            {
                addChild( child );
            }
        }

        // save
        this.types = types;
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
                TYPES_LOG.error( "Module activator '{}' threw exception: {}", this.activator, e.getMessage(), e );
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
        this.types = Collections.emptyMap();
    }

    @Nonnull
    private Map<Class<?>, TypeDescriptor> createComponentsMap()
    {
        Map<Class<?>, TypeDescriptor> componentDescriptors = new ConcurrentHashMap<>();
        for( URL resource : this.module.getModuleResources().findResources( "**/*.class" ) )
        {
            String className = resource.getPath().replace( "/", "." );
            className = className.substring( 0, className.length() - ".class".length() );
            if( className.startsWith( "." ) )
            {
                className = className.substring( 1 );
            }

            try
            {
                Class<?> clazz = this.module.getBundle().loadClass( className );
                TypeDescriptor componentDescriptor = new TypeDescriptor( this.module, clazz );
                componentDescriptors.put( clazz, componentDescriptor );
            }
            catch( ClassNotFoundException | NoClassDefFoundError e )
            {
                CLASS_LOAD_ERRORS_LOG.trace( "Could not load class '{}' from module {}: {}", className, this.module, e.getMessage(), e );
            }
        }
        return componentDescriptors;
    }

    @Nonnull
    private SimpleDirectedGraph<TypeDescriptor, ComponentDependency> createComponentsGraph( @Nonnull Map<Class<?>, TypeDescriptor> types )
    {
        SimpleDirectedGraph<TypeDescriptor, ComponentDependency> componentsGraph = new SimpleDirectedGraph<>( ComponentDependency.class );
        for( TypeDescriptor typeDescriptor : types.values() )
        {
            componentsGraph.addVertex( typeDescriptor );

            // a component by definition requires all its superclasses as components too
            Class<?> type = typeDescriptor.getType().getSuperclass();
            while( type != null )
            {
                TypeDescriptor superClassComponent = types.get( type );
                if( superClassComponent != null )
                {
                    componentsGraph.addVertex( superClassComponent );
                    componentsGraph.addEdge( typeDescriptor, superClassComponent );
                }
                type = type.getSuperclass();
            }

            // a component requires wired field components too
            for( TypeDescriptorFieldComponent field : typeDescriptor.getChildren( TypeDescriptorFieldComponent.class, false ) )
            {
                Class<?> requiredComponentType = field.getFieldType();

                // check that component doesn't require itself
                if( requiredComponentType.isAssignableFrom( typeDescriptor.getType() ) )
                {
                    String msg = "type " + typeDescriptor + " requires itself";
                    throw new ComponentDefinitionException( msg, typeDescriptor.getType(), this.module );
                }

                // make this component depend on all other components matching current field
                for( TypeDescriptor candidateDescriptor : types.values() )
                {
                    if( candidateDescriptor != typeDescriptor )
                    {
                        if( requiredComponentType.isAssignableFrom( candidateDescriptor.getType() ) )
                        {
                            componentsGraph.addVertex( candidateDescriptor );
                            componentsGraph.addEdge( typeDescriptor, candidateDescriptor );
                        }
                    }
                }
            }

            // a component requires wired field components too
            for( TypeDescriptorFieldComponentList field : typeDescriptor.getChildren( TypeDescriptorFieldComponentList.class, false ) )
            {
                Class<?> requiredComponentType = field.getFieldListItemType();

                // check that component doesn't require itself
                if( requiredComponentType.isAssignableFrom( typeDescriptor.getType() ) )
                {
                    String msg = "type " + typeDescriptor + " requires itself";
                    throw new ComponentDefinitionException( msg, typeDescriptor.getType(), this.module );
                }

                // make this component depend on all other components matching current field
                for( TypeDescriptor candidateDescriptor : types.values() )
                {
                    if( candidateDescriptor != typeDescriptor )
                    {
                        if( requiredComponentType.isAssignableFrom( candidateDescriptor.getType() ) )
                        {
                            componentsGraph.addVertex( candidateDescriptor );
                            componentsGraph.addEdge( typeDescriptor, candidateDescriptor );
                        }
                    }
                }
            }
        }
        return componentsGraph;
    }
}
