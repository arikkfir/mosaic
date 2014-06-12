package org.mosaic.core.services.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.slf4j.Logger;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
class ServiceTrackerImpl<ServiceType> implements ServiceProvider<ServiceType>,
                                                 ServicesProvider<ServiceType>,
                                                 ServiceTracker<ServiceType>,
                                                 ServiceListener<ServiceType>
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final Module module;

    @Nonnull
    private final Class<ServiceType> type;

    @Nonnull
    private final Module.ServiceProperty[] properties;

    @Nullable
    private List<ServiceListener<ServiceType>> eventHandlers;

    @Nullable
    private List<ServiceRegistration<ServiceType>> registrations;

    @Nullable
    private List<ServiceRegistration<ServiceType>> unmodifiableRegistrations;

    @Nullable
    private List<ServiceType> services;

    @Nullable
    private List<ServiceType> unmodifiableServices;

    @Nullable
    private ServiceListenerRegistration<ServiceType> listenerRegistration;

    ServiceTrackerImpl( @Nonnull ReadWriteLock lock,
                        @Nonnull Logger logger,
                        @Nonnull Module module,
                        @Nonnull Class<ServiceType> type,
                        @Nonnull Module.ServiceProperty... properties )
    {
        this.lock = lock;
        this.logger = logger;
        this.module = module;
        this.type = type;
        this.properties = properties;
    }

    @Override
    public String toString()
    {
        return this.lock.read( () -> ToStringHelper.create( this )
                                                   .add( "type", this.type )
                                                   .add( "properties", asList( this.properties ) )
                                                   .toString() );
    }

    @Override
    public void addEventHandler( @Nonnull ServiceListener<ServiceType> listener )
    {
        this.lock.write( () -> {
            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers == null )
            {
                eventHandlers = new CopyOnWriteArrayList<>();
                this.eventHandlers = eventHandlers;
            }
            eventHandlers.add( listener );

            List<ServiceRegistration<ServiceType>> registrations = this.unmodifiableRegistrations;
            if( registrations != null )
            {
                for( ServiceRegistration<ServiceType> registration : getRegistrations() )
                {
                    try
                    {
                        listener.serviceRegistered( registration );
                    }
                    catch( Throwable e )
                    {
                        logger.warn( "ServiceTracker listener '{}' threw an exception: {}", listener, e.getMessage(), e );
                    }
                }
            }
        } );
    }

    @Override
    public void addEventHandler( @Nonnull ServiceManager.ServiceRegisteredAction<ServiceType> onRegister,
                                 @Nonnull ServiceManager.ServiceUnregisteredAction<ServiceType> onUnregister )
    {
        addEventHandler( new ServiceListener<ServiceType>()
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
        } );
    }

    @Override
    public void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener )
    {
        this.lock.write( () -> {
            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers != null )
            {
                eventHandlers.remove( listener );
            }
        } );
    }

    @Override
    public void startTracking()
    {
        this.lock.write( () -> {
            this.registrations = new LinkedList<>();
            this.services = new LinkedList<>();
            this.unmodifiableRegistrations = Collections.unmodifiableList( this.registrations );
            this.unmodifiableServices = Collections.unmodifiableList( this.services );
            this.listenerRegistration = this.module.addWeakServiceListener( this, this.type, this.properties );
        } );
    }

    @Override
    public void stopTracking()
    {
        this.lock.write( () -> {
            ServiceListenerRegistration<ServiceType> listenerRegistration = this.listenerRegistration;
            if( listenerRegistration != null )
            {
                listenerRegistration.unregister();
                this.listenerRegistration = null;
            }

            this.unmodifiableServices = null;
            this.unmodifiableRegistrations = null;
            this.services = null;
            this.registrations = null;
        } );
    }

    @Nonnull
    @Override
    public List<ServiceRegistration<ServiceType>> getRegistrations()
    {
        return this.lock.read( () -> {
            List<ServiceRegistration<ServiceType>> registrations = this.unmodifiableRegistrations;
            if( registrations == null )
            {
                throw new IllegalStateException( "service tracker not open" );
            }
            return registrations;
        } );
    }

    @Nonnull
    @Override
    public List<ServiceType> getServices()
    {
        return this.lock.read( () -> {
            List<ServiceType> services = this.unmodifiableServices;
            if( services == null )
            {
                throw new IllegalStateException( "service tracker not open" );
            }
            return services;
        } );
    }

    @Nullable
    @Override
    public ServiceRegistration<ServiceType> getRegistration()
    {
        return this.lock.read( () -> {
            List<ServiceRegistration<ServiceType>> registrations = getRegistrations();
            return registrations.isEmpty() ? null : registrations.get( 0 );
        } );
    }

    @Nullable
    @Override
    public ServiceType getService()
    {
        return this.lock.read( () -> {
            List<ServiceType> services = getServices();
            return services.isEmpty() ? null : services.get( 0 );
        } );
    }

    @Override
    public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        this.lock.write( () -> {
            List<ServiceRegistration<ServiceType>> registrations = this.registrations;
            if( registrations != null )
            {
                registrations.add( registration );
            }

            List<ServiceType> services = this.services;
            if( services != null )
            {
                services.add( registration.getService() );
            }

            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers != null )
            {
                for( ServiceListener<ServiceType> eventHandler : eventHandlers )
                {
                    try
                    {
                        eventHandler.serviceRegistered( registration );
                    }
                    catch( Throwable e )
                    {
                        logger.warn( "ServiceTracker listener '{}' threw an exception: {}", eventHandler, e.getMessage(), e );
                    }
                }
            }
        } );
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                     @Nonnull ServiceType service )
    {
        this.lock.write( () -> {
            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers != null )
            {
                for( ServiceListener<ServiceType> eventHandler : eventHandlers )
                {
                    try
                    {
                        eventHandler.serviceUnregistered( registration, service );
                    }
                    catch( Throwable e )
                    {
                        logger.warn( "ServiceTracker listener '{}' threw an exception: {}", eventHandler, e.getMessage(), e );
                    }
                }
            }

            List<ServiceType> services = this.services;
            if( services != null )
            {
                services.remove( service );
            }

            List<ServiceRegistration<ServiceType>> registrations = this.registrations;
            if( registrations != null )
            {
                registrations.remove( registration );
            }
        } );
    }
}
