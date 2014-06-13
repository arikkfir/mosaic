package org.mosaic.core.services.impl;

import java.util.*;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.workflow.Workflow;
import org.osgi.framework.Filter;
import org.slf4j.Logger;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
public class ServiceManagerImpl implements ServiceManagerEx
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

    public ServiceManagerImpl( @Nonnull Workflow workflow, @Nonnull Logger logger, @Nonnull ReadWriteLock lock )
    {
        this.lock = lock;
        this.logger = logger;
        workflow.addAction(
                ServerStatus.STARTED,
                c -> {
                    this.services = new HashMap<>();
                    this.listeners = new LinkedList<>();
                },
                c -> {
                    this.listeners = null;
                    this.services = null;
                } );
        workflow.addAction(
                ServerStatus.STOPPED,
                c -> {
                    this.listeners = null;
                    this.services = null;
                } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                               @Nonnull ServiceListener<ServiceType> listener,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull Module.ServiceProperty... properties )
    {
        return addListenerEntry( new ServiceListenerAdapter( this.logger, this.lock, this, module, listener, type, properties ) );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                               @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                                                               @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull Module.ServiceProperty... properties )
    {
        return addListener( module,
                            new ServiceListener<ServiceType>()
                            {
                                @Override
                                public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
                                {
                                    onRegister.serviceRegistered( registration );
                                }

                                @Override
                                public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                                                 @Nonnull ServiceType service )
                                {
                                    onUnregister.serviceUnregistered( registration, service );
                                }
                            },
                            type,
                            properties );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addWeakListener( @Nullable Module module,
                                                                                   @Nonnull ServiceListener<ServiceType> listener,
                                                                                   @Nonnull Class<ServiceType> type,
                                                                                   @Nonnull Module.ServiceProperty... properties )
    {
        return addListenerEntry( new WeakServiceListenerAdapter( this.logger, this.lock, this, module, listener, type, properties ) );
    }

    @Override
    @Nullable
    public <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                       @Nonnull Module.ServiceProperty... properties )
    {
        return this.lock.read( () -> {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            // TODO: cache results by type+filter, clear cache on new services
            Filter filter = createFilter( properties );
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
        } );
    }

    @Override
    @Nonnull
    public <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Module module,
                                                                           @Nonnull Class<ServiceType> type,
                                                                           @Nonnull Module.ServiceProperty... properties )
    {
        //noinspection Convert2Diamond
        return this.lock.read( () -> new ServiceTrackerImpl<ServiceType>( this.lock, this.logger, module, type, properties ) );
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
        return this.lock.write( () -> {
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
        } );
    }

    @Override
    public void unregisterServicesFrom( @Nonnull Module module )
    {
        this.lock.write( () -> {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services != null )
            {
                new LinkedList<>( services.keySet() )
                        .stream()
                        .filter( registration -> module.equals( registration.getProvider() ) )
                        .forEach( ServiceRegistrationImpl::unregister );
            }
        } );
    }

    @Override
    public void unregisterListenersFrom( @Nonnull Module module )
    {
        this.lock.write( () -> {
            List<BaseServiceListenerAdapter> listeners = this.listeners;
            if( listeners != null )
            {
                new LinkedList<>( listeners )
                        .stream()
                        .filter( listener -> module.equals( listener.getModule() ) )
                        .forEach( BaseServiceListenerAdapter::unregister );
            }
        } );
    }

    @Nullable
    Map<ServiceRegistrationImpl, Object> getServices()
    {
        return this.lock.read( () -> this.services );
    }

    @Nullable
    List<BaseServiceListenerAdapter> getListeners()
    {
        return this.lock.read( () -> this.listeners );
    }

    private <ServiceType> ServiceListenerRegistration<ServiceType> addListenerEntry( @Nonnull BaseServiceListenerAdapter<ServiceType> listenerAdapter )
    {
        return this.lock.write( () -> {
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
        } );
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
