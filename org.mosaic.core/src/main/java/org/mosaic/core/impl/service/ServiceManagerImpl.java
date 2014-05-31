package org.mosaic.core.impl.service;

import java.util.*;
import org.mosaic.core.*;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.Filter;
import org.slf4j.Logger;

/**
 * @author arik
 * @feature refactor 'addListener' methods to return "ListenerRegistration&lt;?&gt;" instance with an "unregister" method
 */
@SuppressWarnings( "unchecked" )
public class ServiceManagerImpl extends TransitionAdapter implements ServiceManagerEx
{
    @Nullable
    static Filter createFilter( @Nonnull Module.ServiceProperty... properties )
    {
        if( properties.length == 0 )
        {
            return null;
        }

        FilterBuilder filterBuilder = new FilterBuilder();
        for( Module.ServiceProperty property : properties )
        {
            filterBuilder.addEquals( property.getName(), Objects.toString( property.getValue(), "" ) );
        }
        return filterBuilder.toFilter();
    }

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final Logger logger;

    @Nullable
    private Map<ServiceRegistrationImpl, Object> services;

    @Nullable
    private List<BaseServiceListenerAdapter> listeners;

    public ServiceManagerImpl( @Nonnull Logger logger, @Nonnull ReadWriteLock lock )
    {
        this.lock = lock;
        this.logger = logger;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        this.lock.acquireWriteLock();
        try
        {
            if( target == ServerStatus.STARTED )
            {
                this.services = new HashMap<>();
                this.listeners = new LinkedList<>();
            }
            else if( target == ServerStatus.STOPPED )
            {
                this.listeners = null;
                this.services = null;
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        this.lock.acquireWriteLock();
        try
        {
            if( target == ServerStatus.STARTED )
            {
                this.listeners = null;
                this.services = null;
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public <ServiceType> ListenerRegistration<ServiceType> addListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                        @Nonnull Class<ServiceType> type,
                                                                        @Nonnull Module.ServiceProperty... properties )
    {
        return addListenerEntry( new ServiceListenerAdapter( this.logger, this.lock, this, listener, type, properties ) );
    }

    @Override
    public <ServiceType> ListenerRegistration<ServiceType> addWeakListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                            @Nonnull Class<ServiceType> type,
                                                                            @Nonnull Module.ServiceProperty... properties )
    {
        return addListenerEntry( new WeakServiceListenerAdapter( this.logger, this.lock, this, listener, type, properties ) );
    }

    @Override
    @Nullable
    public <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                       @Nonnull Module.ServiceProperty... properties )
    {
        this.lock.acquireReadLock();
        try
        {
            Filter filter = createFilter( properties );

            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            // TODO: cache results by type+filter, clear cache on new services
            for( ServiceRegistrationImpl registration : services.keySet() )
            {
                if( registration.getType().equals( type ) )
                {
                    if( filter == null || filter.matches( registration.getProperties() ) )
                    {
                        return registration;
                    }
                }
            }
            return null;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Override
    @Nonnull
    public <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull Module.ServiceProperty... properties )
    {
        return new ServiceTrackerImpl<>( this.lock, this.logger, this, type, properties );
    }

    @Override
    @Nonnull
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                           @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nonnull Module.ServiceProperty... properties )
    {
        return registerService( module, type, service, createMap( properties ) );
    }

    @Override
    @Nonnull
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                           @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nullable Map<String, Object> properties )
    {
        this.lock.acquireWriteLock();
        try
        {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            List<BaseServiceListenerAdapter> listeners = this.listeners;
            if( listeners == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            Map<String, Object> propertyMap = properties == null ? Collections.<String, Object>emptyMap() : properties;
            ServiceRegistrationImpl<ServiceType> registration = new ServiceRegistrationImpl<>( this.lock, this.logger, this, module, type, propertyMap );
            services.put( registration, service );
            this.logger.trace( "Registered service {}", registration );

            for( ServiceListener listener : listeners )
            {
                try
                {
                    listener.serviceRegistered( registration );
                }
                catch( Exception e )
                {
                    this.logger.warn( "Service listener '{}' threw an exception reacting to service registration", listener, e );
                }
            }

            return registration;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void unregisterServicesFrom( @Nonnull Module module )
    {
        this.lock.acquireWriteLock();
        try
        {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services != null )
            {
                for( ServiceRegistrationImpl registration : services.keySet() )
                {
                    if( registration.getProvider().equals( module ) )
                    {
                        registration.unregister();
                    }
                }
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nullable
    Map<ServiceRegistrationImpl, Object> getServices()
    {
        return this.services;
    }

    @Nullable
    List<BaseServiceListenerAdapter> getListeners()
    {
        return this.listeners;
    }

    private <ServiceType> ListenerRegistration<ServiceType> addListenerEntry( @Nonnull BaseServiceListenerAdapter<ServiceType> listenerAdapter )
    {
        this.lock.acquireWriteLock();
        try
        {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            List<BaseServiceListenerAdapter> listeners = this.listeners;
            if( listeners == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            listeners.add( listenerAdapter );
            this.logger.trace( "Registered service listener {}", listenerAdapter );

            // TODO: add caching to only call with services of the listener's requested type (instead of letting the adapter filter them each time)
            for( ServiceRegistrationImpl registration : services.keySet() )
            {
                listenerAdapter.serviceRegistered( registration );
            }
            return listenerAdapter;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    private Map<String, Object> createMap( @Nonnull Module.ServiceProperty... properties )
    {
        if( properties.length == 0 )
        {
            return Collections.emptyMap();
        }

        Map<String, Object> propertyMap = new HashMap<>();
        for( Module.ServiceProperty property : properties )
        {
            propertyMap.put( property.getName(), property.getValue() );
        }
        return propertyMap;
    }
}
