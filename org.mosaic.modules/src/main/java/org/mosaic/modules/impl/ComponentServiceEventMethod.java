package org.mosaic.modules.impl;

import com.google.common.base.Optional;
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
import org.apache.commons.lang3.tuple.Pair;
import org.mosaic.modules.*;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.modules.impl.Activator.getServiceAndFilterFromType;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentServiceEventMethod extends Lifecycle
        implements ServiceTrackerCustomizer, Module.ServiceRequirement
{
    private static final Logger LOG = LoggerFactory.getLogger( ComponentServiceEventMethod.class );

    @Nonnull
    private final Component component;

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

    ComponentServiceEventMethod( @Nonnull Component component, @Nonnull Method method )
    {
        this.component = component;
        this.method = method;
        this.method.setAccessible( true );

        Pair<Class<?>, FilterBuilder> pair = getServiceAndFilterFromType( this.component.getModule(), this.component.getType(), getServiceType() );
        this.serviceType = pair.getKey();

        // check if are to invoke this method when services become available
        OnServiceAdded onServiceAddedAnn = this.method.getAnnotation( OnServiceAdded.class );
        this.invokeOnAdd = onServiceAddedAnn != null;
        if( this.invokeOnAdd )
        {
            for( OnServiceAdded.P property : onServiceAddedAnn.properties() )
            {
                pair.getRight().addEquals( property.key(), property.value() );
            }
        }

        // check if are to invoke this method when services become UNavailable
        OnServiceRemoved onServiceRemovedAnn = this.method.getAnnotation( OnServiceRemoved.class );
        this.invokeOnRemove = onServiceRemovedAnn != null;
        if( this.invokeOnRemove )
        {
            for( OnServiceRemoved.P property : onServiceRemovedAnn.properties() )
            {
                pair.getRight().addEquals( property.key(), property.value() );
            }
        }

        // ensure method does not have both @OnServiceAdded and @OnServiceRemoved
        if( this.invokeOnAdd && this.invokeOnRemove )
        {
            String msg = "@OnService[Added/Removed] method " + this + " specifies both @OnServiceAdded and @OnServiceRemoved";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }

        // create our service tracker
        this.filter = pair.getRight().toString();
        try
        {
            BundleContext bundleContext = this.component.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + this.component.getModule() );
            }

            Filter filter = FrameworkUtil.createFilter( pair.getRight().toString() );
            this.serviceTracker = new ServiceTracker( bundleContext, filter, this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "@OnService[Added/Removed] method " + this + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }
    }

    @Override
    public final String toString()
    {
        return "ComponentServiceEventMethod[" + this.method.getName() + " in " + this.component + "]";
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.component.getModule();
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
        BundleContext bundleContext = this.component.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + this.component.getModule() );
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
                    this.method.invoke( this.component.getInstance(), mosaicReference );
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
                    this.method.invoke( this.component.getInstance(), mosaicReference );
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

    @Nonnull
    private Type getServiceType()
    {
        Type[] parameterTypes = this.method.getGenericParameterTypes();
        if( parameterTypes.length != 1 )
        {
            String msg = "@OnService[Added/Removed] method " + this + " must have exactly 1 parameter";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }

        Type serviceRefType = parameterTypes[ 0 ];
        if( !( serviceRefType instanceof ParameterizedType ) )
        {
            String msg = "@OnService[Added/Removed] method " + this + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }

        ParameterizedType parameterizedServiceRefType = ( ParameterizedType ) serviceRefType;
        if( !( parameterizedServiceRefType.getRawType() instanceof Class<?> ) )
        {
            String msg = "@OnService[Added/Removed] method " + this + " specifies non-concrete service reference parameter - must be 'ServiceReference<...>'";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }

        Class<?> serviceRefClassType = ( Class<?> ) parameterizedServiceRefType.getRawType();
        if( !serviceRefClassType.isAssignableFrom( ServiceReference.class ) )
        {
            String msg = "@OnService[Added/Removed] method " + this + " does not receive 'ServiceReference<...>'";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }

        Type[] serviceRefTypeArguments = parameterizedServiceRefType.getActualTypeArguments();
        if( serviceRefTypeArguments.length == 0 )
        {
            String msg = "@OnService[Added/Removed] method " + this + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, this.component.getType(), this.component.getModule() );
        }
        return serviceRefTypeArguments[ 0 ];
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
            return ComponentServiceEventMethod.this.serviceType;
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return Activator.getModuleManager().getModule( this.reference.getBundle().getBundleId() ).orNull();
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

        @Nonnull
        @Override
        public Optional<?> service()
        {
            return Optional.fromNullable( this.service );
        }
    }
}
