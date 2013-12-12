package org.mosaic.modules.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.pair.Pair;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.modules.impl.ComponentDescriptorImpl.getServiceAndFilterFromType;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentServiceEventMethodLifecycle extends Lifecycle
        implements ServiceTrackerCustomizer, ModuleWiring.ServiceRequirement
{
    private static final Logger LOG = LoggerFactory.getLogger( ComponentServiceEventMethodLifecycle.class );

    @Nonnull
    private final ComponentDescriptorImpl<?> componentDescriptor;

    @Nonnull
    private final Method method;

    @Nonnull
    private final Class serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final ServiceTracker serviceTracker;

    private final boolean invokeOnAdd;

    private final boolean invokeOnRemove;

    @Nonnull
    private final Map<Long, ServiceReferenceImpl> references = new ConcurrentHashMap<>();

    ComponentServiceEventMethodLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor,
                                          @Nonnull Method method )
    {
        this.componentDescriptor = componentDescriptor;
        this.method = method;
        this.method.setAccessible( true );

        Type[] parameterTypes = this.method.getGenericParameterTypes();
        if( parameterTypes.length != 1 )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " must have exactly 1 parameter";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        java.lang.reflect.Type serviceRefType = parameterTypes[ 0 ];
        if( !( serviceRefType instanceof ParameterizedType ) )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        ParameterizedType parameterizedServiceRefType = ( ParameterizedType ) serviceRefType;
        if( !( parameterizedServiceRefType.getRawType() instanceof Class<?> ) )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " specifies non-concrete service reference parameter - must be 'ServiceReference<...>'";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        Class<?> serviceRefClassType = ( Class<?> ) parameterizedServiceRefType.getRawType();
        if( !serviceRefClassType.isAssignableFrom( ServiceReference.class ) )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " does not receive 'ServiceReference<...>'";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        java.lang.reflect.Type[] serviceRefTypeArguments = parameterizedServiceRefType.getActualTypeArguments();
        if( serviceRefTypeArguments.length == 0 )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        Pair<Class<?>, FilterBuilder> pair = getServiceAndFilterFromType( componentDescriptor.getModule(), componentDescriptor.getComponentType(), serviceRefTypeArguments[ 0 ] );
        this.serviceType = pair.getKey();
        FilterBuilder filterBuilder = pair.getRight();

        OnServiceAdded onServiceAddedAnn = this.method.getAnnotation( OnServiceAdded.class );
        if( onServiceAddedAnn != null )
        {
            this.invokeOnAdd = true;
            for( OnServiceAdded.P property : onServiceAddedAnn.properties() )
            {
                filterBuilder.addEquals( property.key(), property.value() );
            }
        }
        else
        {
            this.invokeOnAdd = false;
        }

        OnServiceRemoved onServiceRemovedAnn = this.method.getAnnotation( OnServiceRemoved.class );
        if( onServiceRemovedAnn != null )
        {
            this.invokeOnRemove = true;
            for( OnServiceRemoved.P property : onServiceRemovedAnn.properties() )
            {
                filterBuilder.addEquals( property.key(), property.value() );
            }
        }
        else
        {
            this.invokeOnRemove = false;
        }
        this.filter = filterBuilder.toString();

        if( this.invokeOnAdd && this.invokeOnRemove )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " specifies both @OnServiceAdded and @OnServiceRemoved";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        try
        {
            BundleContext bundleContext = componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
            }
            Filter filter = FrameworkUtil.createFilter( this.filter );
            this.serviceTracker = new ServiceTracker( bundleContext, filter, this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "@OnService[Added/Removed] method '" + this.method.getName() + "' of component " + this.method.getDeclaringClass().getName() + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }
    }

    @Override
    public final String toString()
    {
        return "ComponentServiceEventMethod[" + this.method.getName() + " in " + this.componentDescriptor + "]";
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.componentDescriptor.getModule();
    }

    @Nonnull
    @Override
    public Class<?> getType()
    {
        return this.serviceType;
    }

    @Nullable
    @Override
    public String getFilter()
    {
        return this.filter;
    }

    @Nonnull
    @Override
    public List<ServiceReference<?>> getReferences()
    {
        org.osgi.framework.ServiceReference[] tracked = this.serviceTracker.getServiceReferences();
        if( tracked == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<ServiceReference<?>> serviceReferences = new LinkedList<>();
            for( org.osgi.framework.ServiceReference reference : tracked )
            {
                serviceReferences.add( new ServiceReferenceImpl( reference, this.serviceTracker.getService( reference ) ) );
            }
            return serviceReferences;
        }
    }

    @Override
    public Object addingService( @Nonnull org.osgi.framework.ServiceReference reference )
    {
        BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
        }

        Object service = bundleContext.getService( reference );
        if( service != null )
        {
            ServiceReferenceImpl mosaicReference = new ServiceReferenceImpl( reference, service );
            this.references.put( ( Long ) reference.getProperty( Constants.SERVICE_ID ), mosaicReference );
            if( this.invokeOnAdd )
            {
                try
                {
                    ComponentSingletonLifecycle singletonLifecycle = this.componentDescriptor.requireChild( ComponentSingletonLifecycle.class );
                    this.method.invoke( singletonLifecycle.getInstance(), mosaicReference );
                }
                catch( Throwable e )
                {
                    if( e instanceof InvocationTargetException )
                    {
                        e = e.getCause();
                    }
                    LOG.error( "@OnServiceAdded method '{}' failed: {}", this.method.getName(), e.getMessage(), e );
                }
            }
        }
        return service;
    }

    @Override
    public void modifiedService( @Nonnull org.osgi.framework.ServiceReference reference, @Nonnull Object service )
    {
        // no-op
    }

    @Override
    public void removedService( @Nonnull org.osgi.framework.ServiceReference reference, @Nonnull Object service )
    {
        @SuppressWarnings("RedundantCast")
        ServiceReferenceImpl mosaicReference = this.references.remove( ( Long ) reference.getProperty( Constants.SERVICE_ID ) );
        if( mosaicReference != null )
        {
            if( this.invokeOnRemove )
            {
                try
                {
                    ComponentSingletonLifecycle singletonLifecycle = this.componentDescriptor.requireChild( ComponentSingletonLifecycle.class );
                    this.method.invoke( singletonLifecycle.getInstance(), mosaicReference );
                }
                catch( Throwable e )
                {
                    LOG.error( "@OnServiceRemoved method '{}' failed: {}", this.method.getName(), e.getMessage(), e );
                }
            }
        }
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        this.serviceTracker.open();
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        this.serviceTracker.close();
    }

    private class ServiceReferenceImpl implements ServiceReference
    {
        @Nonnull
        private final org.osgi.framework.ServiceReference<?> reference;

        private final Object service;

        private ServiceReferenceImpl( @Nonnull org.osgi.framework.ServiceReference<?> reference,
                                      @Nonnull Object service )
        {
            this.reference = reference;
            this.service = service;
        }

        @Override
        public long getId()
        {
            return ( Long ) this.reference.getProperty( Constants.SERVICE_ID );
        }

        @Nonnull
        @Override
        public Class getType()
        {
            return ComponentServiceEventMethodLifecycle.this.serviceType;
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return Activator.getModuleManager().getModule( this.reference.getBundle().getBundleId() );
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            String[] propertyKeys = this.reference.getPropertyKeys();

            MapEx<String, Object> properties = new HashMapEx<>( propertyKeys.length );
            for( String key : propertyKeys )
            {
                properties.put( key, this.reference.getProperty( key ) );
            }
            return properties;
        }

        @Nullable
        @Override
        public Object get()
        {
            return this.service;
        }

        @Nonnull
        @Override
        public Object require()
        {
            Object service = get();
            if( service != null )
            {
                return service;
            }
            else
            {
                String typeName = ComponentServiceEventMethodLifecycle.this.serviceType.getName();
                throw new IllegalStateException( "service of type '" + typeName + "' is not available" );
            }
        }
    }
}
