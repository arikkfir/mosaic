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
    private List<ServiceType> services;

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

            if( this.registrations != null )
            {
                for( ServiceRegistration<ServiceType> registration : this.registrations )
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
    public void addEventHandler( @Nullable ServiceRegistrationListener<ServiceType> onRegister,
                                 @Nullable ServiceUnregistrationListener<ServiceType> onUnregister )
    {
        addEventHandler( new ServiceListener<ServiceType>()
        {
            @Override
            public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
            {
                if( onRegister != null )
                {
                    onRegister.serviceRegistered( registration );
                }
            }

            @Override
            public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                             @Nonnull ServiceType service )
            {
                if( onUnregister != null )
                {
                    onUnregister.serviceUnregistered( registration, service );
                }
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

            this.services = null;
            this.registrations = null;
        } );
    }

    @Nonnull
    @Override
    public List<ServiceRegistration<ServiceType>> getRegistrations()
    {
        return this.lock.read( () -> {
            if( this.registrations != null )
            {
                List<ServiceRegistration<ServiceType>> registrations = new LinkedList<>();
                registrations.addAll( this.registrations );
                return registrations;
            }
            else
            {
                return Collections.<ServiceRegistration<ServiceType>>emptyList();
            }
        } );
    }

    @Nonnull
    @Override
    public List<ServiceType> getServices()
    {
        return this.lock.read( () -> {
            if( this.services != null )
            {
                List<ServiceType> services = new LinkedList<>();
                services.addAll( this.services );
                return services;
            }
            else
            {
                return Collections.<ServiceType>emptyList();
            }
        } );
    }

    @Nullable
    @Override
    public ServiceRegistration<ServiceType> getRegistration()
    {
        return this.lock.read( () -> this.registrations == null ? null : this.registrations.get( 0 ) );
    }

    @Nullable
    @Override
    public ServiceType getService()
    {
        return this.lock.read( () -> this.services == null || this.services.isEmpty() ? null : this.services.get( 0 ) );
    }

    @Override
    public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        this.lock.write( () -> {

            if( this.registrations != null )
            {
                this.registrations.add( registration );
            }

            if( this.services != null )
            {
                this.services.add( registration.getService() );
            }

            if( this.eventHandlers != null )
            {
                for( ServiceListener<ServiceType> eventHandler : this.eventHandlers )
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

    @SuppressWarnings( "SuspiciousMethodCalls" )
    @Override
    public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                     @Nonnull ServiceType service )
    {
        this.lock.write( () -> {

            if( this.eventHandlers != null )
            {
                for( ServiceListener<ServiceType> eventHandler : this.eventHandlers )
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

            if( this.services != null )
            {
                this.services.remove( service );
            }

            if( this.registrations != null )
            {
                this.registrations.remove( registration );
            }
        } );
    }
}
