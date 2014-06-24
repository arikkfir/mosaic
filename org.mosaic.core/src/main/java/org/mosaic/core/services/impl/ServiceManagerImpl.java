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

import static java.util.Objects.requireNonNull;
import static org.mosaic.core.impl.Activator.getDispatcher;

/**
 * @author arik
 * @todo refactor data structures for performance (cache services & listeners by type and filters, clear on new registrations)
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
    private static Map<String, Object> createMap( @Nonnull Module.ServiceProperty... properties )
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
        //noinspection Convert2Diamond
        return addListener( module, new CompositeListener<ServiceType>( onRegister, onUnregister ), type, properties );
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
            Filter filter = createFilter( properties );
            for( ServiceRegistrationImpl registration : requireNonNull( this.services ).keySet() )
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
        ServiceRegistrationImpl<ServiceType> registration =
                new ServiceRegistrationImpl<>(
                        this.lock,
                        this,
                        module,
                        type,
                        properties == null ? Collections.<String, Object>emptyMap() : properties );

        requireNonNull( getDispatcher() ).dispatch( () -> {

            requireNonNull( this.services ).put( registration, service );
            this.logger.trace( "Registered a service of {}: {}", registration.getType().getName(), service );

            for( ServiceListener listener : requireNonNull( this.listeners ) )
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
        } );

        return registration;
    }

    @Override
    public void unregisterServicesFrom( @Nonnull Module module )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> requireNonNull( this.services ).keySet().stream()
                                                                                         .filter( registration -> module.equals( registration.getProvider() ) )
                                                                                         .forEach( this::unregisterService ) );
    }

    @Override
    public void unregisterListenersFrom( @Nonnull Module module )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> requireNonNull( this.listeners ).stream()
                                                                                          .filter( listener -> module.equals( listener.getModule() ) )
                                                                                          .forEach( this::unregisterListener ) );
    }

    void unregisterService( ServiceRegistrationImpl<?> registration )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> {

            Object service = requireNonNull( this.services ).remove( registration );
            this.logger.trace( "Unregistered service {}", registration );

            for( ServiceListener listener : requireNonNull( this.listeners ) )
            {
                try
                {
                    listener.serviceUnregistered( registration, service );
                }
                catch( Exception e )
                {
                    this.logger.warn( "Service listener '{}' threw an exception reacting to service unregistration", listener, e );
                }
            }
        } );
    }

    void unregisterListener( BaseServiceListenerAdapter<?> registration )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> {

            requireNonNull( this.listeners ).remove( registration );
            this.logger.trace( "Removed listener entry {}", registration );

        } );
    }

    @Nullable
    Object getServiceInstanceFor( @Nonnull ServiceRegistrationImpl<?> registration )
    {
        return this.lock.read( () -> requireNonNull( this.services ).get( registration ) );
    }

    private <ServiceType> ServiceListenerRegistration<ServiceType> addListenerEntry( @Nonnull BaseServiceListenerAdapter<ServiceType> listenerAdapter )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> {

            requireNonNull( this.listeners ).add( listenerAdapter );
            this.logger.trace( "Registered service listener {}", listenerAdapter );

            requireNonNull( this.services ).keySet().forEach( listenerAdapter::serviceRegistered );
        } );
        return listenerAdapter;
    }
}
