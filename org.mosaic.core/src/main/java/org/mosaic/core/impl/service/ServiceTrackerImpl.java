package org.mosaic.core.impl.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.core.*;
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
    private final ServiceManagerImpl serviceManager;

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

    ServiceTrackerImpl( @Nonnull ReadWriteLock lock,
                        @Nonnull Logger logger,
                        @Nonnull ServiceManagerImpl serviceManager,
                        @Nonnull Class<ServiceType> type,
                        @Nonnull Module.ServiceProperty... properties )
    {
        this.lock = lock;
        this.logger = logger;
        this.serviceManager = serviceManager;
        this.type = type;
        this.properties = properties;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.type )
                             .add( "properties", asList( this.properties ) )
                             .toString();
    }

    @Override
    public void addEventHandler( @Nonnull ServiceListener<ServiceType> listener )
    {
        this.lock.acquireWriteLock();
        try
        {
            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers == null )
            {
                eventHandlers = new CopyOnWriteArrayList<>();
                this.eventHandlers = eventHandlers;
            }
            eventHandlers.add( listener );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener )
    {
        this.lock.acquireWriteLock();
        try
        {
            List<ServiceListener<ServiceType>> eventHandlers = this.eventHandlers;
            if( eventHandlers != null )
            {
                eventHandlers.remove( listener );
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void startTracking()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.registrations = new LinkedList<>();
            this.services = new LinkedList<>();
            this.unmodifiableRegistrations = Collections.unmodifiableList( this.registrations );
            this.unmodifiableServices = Collections.unmodifiableList( this.services );
            this.serviceManager.addWeakListener( this, this.type, this.properties );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Override
    public void stopTracking()
    {
        this.lock.acquireWriteLock();
        try
        {
            this.serviceManager.removeListener( this );
            this.unmodifiableServices = null;
            this.unmodifiableRegistrations = null;
            this.services = null;
            this.registrations = null;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    @Override
    public List<ServiceRegistration<ServiceType>> getRegistrations()
    {
        this.lock.acquireReadLock();
        try
        {
            List<ServiceRegistration<ServiceType>> registrations = this.unmodifiableRegistrations;
            if( registrations == null )
            {
                throw new IllegalStateException( "service tracker not open" );
            }
            return registrations;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public List<ServiceType> getServices()
    {
        this.lock.acquireReadLock();
        try
        {
            List<ServiceType> services = this.unmodifiableServices;
            if( services == null )
            {
                throw new IllegalStateException( "service tracker not open" );
            }
            return services;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ServiceRegistration<ServiceType> getRegistration()
    {
        List<ServiceRegistration<ServiceType>> registrations = getRegistrations();
        if( registrations.isEmpty() )
        {
            return null;
        }
        else
        {
            return registrations.get( 0 );
        }
    }

    @Nullable
    @Override
    public ServiceType getService()
    {
        this.lock.acquireReadLock();
        try
        {
            List<ServiceType> services = getServices();
            if( services.isEmpty() )
            {
                return null;
            }
            else
            {
                return services.get( 0 );
            }
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Override
    public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        this.lock.acquireWriteLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @SuppressWarnings( "SuspiciousMethodCalls" )
    @Override
    public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                     @Nonnull ServiceType service )
    {
        this.lock.acquireWriteLock();
        try
        {
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
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }
}
