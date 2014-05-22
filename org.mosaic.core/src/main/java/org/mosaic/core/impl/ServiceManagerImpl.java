package org.mosaic.core.impl;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.core.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.Filter;

/**
 * @author arik
 * @feature refactor 'addListener' methods to return "ListenerRegistration&lt;?&gt;" instance with an "unregister" method
 */
@SuppressWarnings( "unchecked" )
class ServiceManagerImpl extends TransitionAdapter implements ServiceManager
{
    @Nonnull
    private final ServerImpl server;

    @Nullable
    private Map<ServiceRegistrationImpl, Object> services;

    @Nullable
    private List<BaseServiceListenerAdapter> serviceListeners;

    ServiceManagerImpl( @Nonnull ServerImpl server )
    {
        this.server = server;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            this.services = new HashMap<>();
            this.serviceListeners = new LinkedList<>();
        }
        else if( target == ServerStatus.STOPPED )
        {
            this.serviceListeners = null;
            this.services = null;
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            this.serviceListeners = null;
            this.services = null;
        }
    }

    @Override
    public <ServiceType> void addListener( @Nonnull ServiceListener<ServiceType> listener,
                                           @Nonnull Class<ServiceType> type,
                                           @Nonnull Module.ServiceProperty... properties )
    {
        addListener( listener, type, createFilter( properties ) );
    }

    @Override
    public <ServiceType> void addWeakListener( @Nonnull ServiceListener<ServiceType> listener,
                                               @Nonnull Class<ServiceType> type,
                                               @Nonnull Module.ServiceProperty... properties )
    {
        addWeakListener( listener, type, createFilter( properties ) );
    }

    @Override
    public void removeListener( @Nonnull ServiceListener<?> listener )
    {
        this.server.acquireWriteLock();
        try
        {
            List<BaseServiceListenerAdapter> listeners = this.serviceListeners;
            if( listeners != null )
            {
                Iterator<BaseServiceListenerAdapter> iterator = listeners.iterator();
                while( iterator.hasNext() )
                {
                    BaseServiceListenerAdapter adapter = iterator.next();
                    ServiceListener listenerInAdapter = adapter.getListener();
                    if( listenerInAdapter == null )
                    {
                        this.server.getLogger().trace( "Removed listener entry {} (reference lost)", adapter );
                        iterator.remove();
                    }
                    else if( listenerInAdapter == listener )
                    {
                        this.server.getLogger().trace( "Removed listener entry {}", adapter );
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        finally
        {
            this.server.releaseWriteLock();
        }
    }

    @Override
    @Nullable
    public <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                       @Nonnull Module.ServiceProperty... properties )
    {
        return findService( type, createFilter( properties ) );
    }

    @Override
    @Nonnull
    public <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull Module.ServiceProperty... properties )
    {
        return createServiceTracker( type, createFilter( properties ) );
    }

    <ServiceType> void addListener( @Nonnull ServiceListener<ServiceType> listener,
                                    @Nonnull Class<ServiceType> type,
                                    @Nullable Filter filter )
    {
        addListenerEntry( new ServiceListenerAdapter( listener, type, filter ) );
    }

    <ServiceType> void addWeakListener( @Nonnull ServiceListener<ServiceType> listener,
                                        @Nonnull Class<ServiceType> type,
                                        @Nullable Filter filter )
    {
        addListenerEntry( new WeakServiceListenerAdapter( listener, type, filter ) );
    }

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                    @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nonnull Module.ServiceProperty... properties )
    {
        return registerService( module, type, service, createMap( properties ) );
    }

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                    @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nullable Map<String, Object> properties )
    {
        this.server.acquireWriteLock();
        try
        {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            List<BaseServiceListenerAdapter> listeners = this.serviceListeners;
            if( listeners == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            Map<String, Object> propertyMap = properties == null ? Collections.<String, Object>emptyMap() : properties;
            ServiceRegistrationImpl<ServiceType> registration = new ServiceRegistrationImpl<>( module, type, propertyMap );
            services.put( registration, service );
            this.server.getLogger().trace( "Registered service {}", registration );

            for( ServiceListener listener : listeners )
            {
                try
                {
                    listener.serviceRegistered( registration );
                }
                catch( Exception e )
                {
                    this.server.getLogger().warn( "Service listener '{}' threw an exception reacting to service registration", listener, e );
                }
            }

            return registration;
        }
        finally
        {
            this.server.releaseWriteLock();
        }
    }

    @Nullable
    <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                @Nullable Filter filter )
    {
        this.server.acquireReadLock();
        try
        {
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
            this.server.releaseReadLock();
        }
    }

    @Nonnull
    <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                    @Nullable Filter filter )
    {
        return new ServiceTrackerImpl<>( type, filter );
    }

    private void addListenerEntry( @Nonnull BaseServiceListenerAdapter listenerAdapter )
    {
        this.server.acquireWriteLock();
        try
        {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            List<BaseServiceListenerAdapter> listeners = this.serviceListeners;
            if( listeners == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            listeners.add( listenerAdapter );
            this.server.getLogger().trace( "Registered service listener {}", listenerAdapter );

            // TODO: add caching to only call with services of the listener's requested type (instead of letting the adapter filter them each time)
            for( ServiceRegistrationImpl registration : services.keySet() )
            {
                listenerAdapter.serviceRegistered( registration );
            }
        }
        finally
        {
            this.server.releaseWriteLock();
        }
    }

    @Nullable
    private Filter createFilter( @Nonnull Module.ServiceProperty... properties )
    {
        if( properties.length == 0 )
        {
            return null;
        }

        Util.FilterBuilder filterBuilder = new Util.FilterBuilder();
        for( Module.ServiceProperty property : properties )
        {
            filterBuilder.addEquals( property.getName(), Objects.toString( property.getValue(), "" ) );
        }
        return filterBuilder.toFilter();
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

    private class ServiceRegistrationImpl<ServiceType> implements ServiceRegistration<ServiceType>
    {
        @Nonnull
        private final Module provider;

        @Nonnull
        private final Class<ServiceType> type;

        @Nonnull
        private final Map<String, Object> properties;

        private ServiceRegistrationImpl( @Nonnull Module provider,
                                         @Nonnull Class<ServiceType> type,
                                         @Nonnull Map<String, Object> properties )
        {
            this.provider = provider;
            this.type = type;
            this.properties = properties;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "provider", this.provider )
                                 .add( "type", this.type.getName() )
                                 .toString();

        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            server.acquireReadLock();
            try
            {
                return this.provider;
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        @Nonnull
        @Override
        public Class<ServiceType> getType()
        {
            server.acquireReadLock();
            try
            {
                return this.type;
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        @Nonnull
        @Override
        public Map<String, Object> getProperties()
        {
            server.acquireReadLock();
            try
            {
                return this.properties;
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        @Nullable
        @Override
        public ServiceType getService()
        {
            server.acquireReadLock();
            try
            {
                Map<ServiceRegistrationImpl, Object> services = ServiceManagerImpl.this.services;
                if( services == null )
                {
                    throw new IllegalStateException( "service manager no longer available (is server started?)" );
                }

                Object instance = services.get( this );
                return this.type.cast( instance );
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        @Override
        public void unregister()
        {
            server.acquireWriteLock();
            try
            {
                Map<ServiceRegistrationImpl, Object> services = ServiceManagerImpl.this.services;
                if( services == null )
                {
                    throw new IllegalStateException( "service manager no longer available (is server started?)" );
                }

                List<BaseServiceListenerAdapter> listeners = ServiceManagerImpl.this.serviceListeners;
                if( listeners == null )
                {
                    throw new IllegalStateException( "service manager no longer available (is server started?)" );
                }

                Object service = services.remove( this );
                if( service != null )
                {
                    ServiceManagerImpl.this.server.getLogger().trace( "Unregistered service {}", this );

                    for( ServiceListener listener : listeners )
                    {
                        try
                        {
                            listener.serviceUnregistered( this, service );
                        }
                        catch( Exception e )
                        {
                            server.getLogger().warn( "Service listener '{}' threw an exception reacting to service unregistration", listener, e );
                        }
                    }
                }
            }
            finally
            {
                server.releaseWriteLock();
            }
        }
    }

    private abstract class BaseServiceListenerAdapter<ServiceType> implements ServiceListener<ServiceType>
    {
        @Nonnull
        protected final Class<ServiceType> type;

        @Nullable
        protected final Filter filter;

        protected BaseServiceListenerAdapter( @Nonnull Class<ServiceType> type, @Nullable Filter filter )
        {
            this.type = type;
            this.filter = filter;
        }

        @Override
        public final void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            server.acquireReadLock();
            try
            {
                ServiceListener<ServiceType> listener = getListener();
                if( listener == null )
                {
                    server.releaseReadLock();
                    try
                    {
                        unregister();
                    }
                    finally
                    {
                        server.acquireReadLock();
                    }
                }
                else if( this.type.isAssignableFrom( registration.getType() ) )
                {
                    if( this.filter == null || this.filter.matches( registration.getProperties() ) )
                    {
                        listener.serviceRegistered( registration );
                    }
                }
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            server.acquireReadLock();
            try
            {
                ServiceListener<ServiceType> listener = getListener();
                if( listener == null )
                {
                    // our listener instance was garbage collected - remove ourselves from the listeners list
                    server.releaseReadLock();
                    try
                    {
                        // unregister will acquire writelock for us
                        unregister();
                    }
                    finally
                    {
                        server.acquireReadLock();
                    }
                }
                else if( this.type.isAssignableFrom( registration.getType() ) )
                {
                    if( this.filter == null || this.filter.matches( registration.getProperties() ) )
                    {
                        listener.serviceRegistered( registration );
                    }
                }
            }
            finally
            {
                server.releaseReadLock();
            }
        }

        final void unregister()
        {
            server.acquireWriteLock();
            try
            {
                List<BaseServiceListenerAdapter> listeners = ServiceManagerImpl.this.serviceListeners;
                if( listeners == null )
                {
                    throw new IllegalStateException( "service manager no longer available (is server started?)" );
                }

                Iterator<BaseServiceListenerAdapter> iterator = listeners.iterator();
                while( iterator.hasNext() )
                {
                    if( iterator.next() == this )
                    {
                        iterator.remove();
                    }
                }
            }
            finally
            {
                server.releaseWriteLock();
            }
        }

        @Nullable
        protected abstract ServiceListener<ServiceType> getListener();
    }

    private class ServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
    {
        @Nonnull
        private final ServiceListener<ServiceType> listener;

        private ServiceListenerAdapter( @Nonnull ServiceListener<ServiceType> listener,
                                        @Nonnull Class<ServiceType> type,
                                        @Nullable Filter filter )
        {
            super( type, filter );
            this.listener = listener;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", this.type.getName() )
                                 .add( "filter", this.filter )
                                 .add( "listener", this.listener )
                                 .toString();
        }

        @Nonnull
        @Override
        protected ServiceListener<ServiceType> getListener()
        {
            return this.listener;
        }
    }

    private class WeakServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
    {
        @Nonnull
        private final WeakReference<ServiceListener<ServiceType>> listener;

        private WeakServiceListenerAdapter( @Nonnull ServiceListener<ServiceType> listener,
                                            @Nonnull Class<ServiceType> type,
                                            @Nullable Filter filter )
        {
            super( type, filter );
            this.listener = new WeakReference<>( listener );
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", this.type.getName() )
                                 .add( "filter", this.filter )
                                 .add( "listener", this.listener.get() )
                                 .toString();
        }

        @Nullable
        @Override
        protected ServiceListener<ServiceType> getListener()
        {
            return this.listener.get();
        }
    }

    private class ServiceTrackerImpl<ServiceType> implements ServiceProvider<ServiceType>,
                                                             ServicesProvider<ServiceType>,
                                                             ServiceTracker<ServiceType>,
                                                             ServiceListener<ServiceType>
    {
        @Nonnull
        private final Class<ServiceType> type;

        @Nullable
        private final Filter filter;

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

        private ServiceTrackerImpl( @Nonnull Class<ServiceType> type, @Nullable Filter filter )
        {
            this.type = type;
            this.filter = filter;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", this.type )
                                 .add( "filter", this.filter )
                                 .toString();
        }

        @Override
        public void addEventHandler( @Nonnull ServiceListener<ServiceType> listener )
        {
            server.acquireWriteLock();
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
                server.releaseWriteLock();
            }
        }

        @Override
        public void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener )
        {
            server.acquireWriteLock();
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
                server.releaseWriteLock();
            }
        }

        @Override
        public void startTracking()
        {
            server.acquireWriteLock();
            try
            {
                this.registrations = new LinkedList<>();
                this.services = new LinkedList<>();
                this.unmodifiableRegistrations = Collections.unmodifiableList( this.registrations );
                this.unmodifiableServices = Collections.unmodifiableList( this.services );
                ServiceManagerImpl.this.addWeakListener( this, this.type, this.filter );
            }
            finally
            {
                server.releaseWriteLock();
            }
        }

        @Override
        public void stopTracking()
        {
            server.acquireWriteLock();
            try
            {
                ServiceManagerImpl.this.removeListener( this );
                this.unmodifiableServices = null;
                this.unmodifiableRegistrations = null;
                this.services = null;
                this.registrations = null;
            }
            finally
            {
                server.releaseWriteLock();
            }
        }

        @Nonnull
        @Override
        public List<ServiceRegistration<ServiceType>> getRegistrations()
        {
            server.acquireReadLock();
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
                server.releaseReadLock();
            }
        }

        @Nonnull
        @Override
        public List<ServiceType> getServices()
        {
            server.acquireReadLock();
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
                server.releaseReadLock();
            }
        }

        @Nullable
        @Override
        public ServiceRegistration getRegistration()
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
            server.acquireReadLock();
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
                server.releaseReadLock();
            }
        }

        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            server.acquireWriteLock();
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
                            server.getLogger().warn( "ServiceTracker listener '{}' threw an exception: {}", eventHandler, e.getMessage(), e );
                        }
                    }
                }
            }
            finally
            {
                server.releaseWriteLock();
            }
        }

        @SuppressWarnings( "SuspiciousMethodCalls" )
        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            server.acquireWriteLock();
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
                            server.getLogger().warn( "ServiceTracker listener '{}' threw an exception: {}", eventHandler, e.getMessage(), e );
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
                server.releaseWriteLock();
            }
        }
    }
}
