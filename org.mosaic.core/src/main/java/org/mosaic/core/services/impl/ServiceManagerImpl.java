package org.mosaic.core.services.impl;

import java.lang.ref.WeakReference;
import java.util.*;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.modules.ServiceProperty;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.core.services.impl.FilterBuilder.createFilter;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
public class ServiceManagerImpl
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ServiceManagerImpl.class );

    @Nonnull
    private static Map<String, Object> createMap( @Nonnull ServiceProperty... properties )
    {
        if( properties.length == 0 )
        {
            return Collections.emptyMap();
        }

        Map<String, Object> propertyMap = new HashMap<>();
        for( ServiceProperty property : properties )
        {
            propertyMap.put( property.getName(), property.getValue() );
        }
        return propertyMap;
    }

    @Nonnull
    private final ReadWriteLock lock;

    @Nullable
    private Map<ServiceRegistrationImpl, Object> services;

    @Nullable
    private List<ServiceListenerAdapter> listeners;

    public ServiceManagerImpl( @Nonnull ServerImpl server )
    {
        this.lock = server.getLock();
        server.addStartupHook( bundleContext -> {
            this.services = new HashMap<>();
            this.listeners = new LinkedList<>();
        } );
        server.addShutdownHook( bundleContext -> {
            this.listeners = null;
            this.services = null;
        } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Nonnull
    public <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable ModuleRevision moduleRevision,
                                                                               @Nonnull ServiceListener<ServiceType> listener,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull ServiceProperty... properties )
    {
        return addListenerEntry( new ServiceListenerAdapter( moduleRevision, type, createFilter( properties ) )
        {
            @Nullable
            @Override
            protected ServiceListener getListenerInstance()
            {
                return listener;
            }
        } );
    }

    @Nonnull
    public <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable ModuleRevision moduleRevision,
                                                                               @Nullable ServiceRegistrationListener<ServiceType> onRegister,
                                                                               @Nullable ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull ServiceProperty... properties )
    {
        return addListenerEntry( new ServiceListenerAdapter( moduleRevision, type, createFilter( properties ) )
        {
            @Nonnull
            private final ServiceListener<ServiceType> listener = new CompositeListener<>( onRegister, onUnregister );

            @Nullable
            @Override
            protected ServiceListener getListenerInstance()
            {
                return this.listener;
            }
        } );
    }

    @Nonnull
    public <ServiceType> ServiceListenerRegistration<ServiceType> addWeakListener( @Nullable ModuleRevision moduleRevision,
                                                                                   @Nonnull ServiceListener<ServiceType> listener,
                                                                                   @Nonnull Class<ServiceType> type,
                                                                                   @Nonnull ServiceProperty... properties )
    {
        return addListenerEntry( new ServiceListenerAdapter( moduleRevision, type, createFilter( properties ) )
        {
            @Nonnull
            private final WeakReference<ServiceListener<ServiceType>> reference = new WeakReference<>( listener );

            @Nullable
            @Override
            protected ServiceListener getListenerInstance()
            {
                return this.reference.get();
            }
        } );
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                       @Nonnull ServiceProperty... properties )
    {
        return findService( type, createFilter( properties ) );
    }

    @Nullable
    public <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                       @Nullable Filter filter )
    {
        return this.lock.read( () -> {
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services != null )
            {
                // it's ok to iterate 'services' here because we're not spawning any other action that can modify it
                return ( ServiceRegistration<ServiceType> ) services.keySet().stream()
                                                                    .filter( registration -> registration.getType().equals( type ) )
                                                                    .filter( registration -> filter == null || filter.matches( registration.getProperties() ) )
                                                                    .findFirst()
                                                                    .orElse( null );
            }
            else
            {
                return null;
            }
        } );
    }

    @Nonnull
    public <ServiceType> Collection<? extends ServiceRegistration<ServiceType>> findServices( @Nonnull Class<ServiceType> type,
                                                                                              @Nonnull ServiceProperty... properties )
    {
        return findServices( type, createFilter( properties ) );
    }

    @Nonnull
    public <ServiceType> Collection<? extends ServiceRegistration<ServiceType>> findServices( @Nonnull Class<ServiceType> type,
                                                                                              @Nullable Filter filter )
    {
        return this.lock.read( () -> {

            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( services != null )
            {
                Collection<ServiceRegistration<ServiceType>> matches = new LinkedList<>();

                // it's ok to iterate 'services' here because we're not spawning any other action that can modify it
                services.keySet()
                        .stream()
                        .filter( registration -> registration.getType().equals( type ) )
                        .filter( registration -> filter == null || filter.matches( registration.getProperties() ) )
                        .forEach( registration -> matches.add( ( ServiceRegistration<ServiceType> ) registration ) );

                return matches;
            }
            else
            {
                return Collections.emptyList();
            }
        } );
    }

    @Nonnull
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nullable ModuleRevision moduleRevision,
                                                                           @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return registerService( moduleRevision, type, service, createMap( properties ) );
    }

    @Nonnull
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nullable ModuleRevision moduleRevision,
                                                                           @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nullable Map<String, Object> properties )
    {
        return this.lock.write( () -> {

            @SuppressWarnings("Convert2Diamond")
            ServiceRegistrationImpl<ServiceType> registration =

                    new ServiceRegistrationImpl<ServiceType>(

                            this.lock,
                            moduleRevision,
                            type,
                            properties == null ? Collections.<String, Object>emptyMap() : properties,

                            unregisteringRegistration -> this.lock.write( () -> {

                                Map<ServiceRegistrationImpl, Object> services = this.services;
                                List<ServiceListenerAdapter> listeners = this.listeners;
                                if( services != null && listeners != null )
                                {
                                    Object serviceInstance = services.remove( unregisteringRegistration );
                                    if( serviceInstance != null )
                                    {
                                        LOG.info( "Unregistered {}", this );

                                        // copying 'listeners' into a new list, because a listener might spawn an action
                                        // that will cause it to change while we are still iterating
                                        for( ServiceListenerAdapter listener : new LinkedList<>( listeners ) )
                                        {
                                            listener.serviceUnregistered( unregisteringRegistration, serviceInstance );
                                        }
                                    }
                                }
                            } ),

                            registrationToGetServiceFor -> this.lock.read( () -> {
                                Map<ServiceRegistrationImpl, Object> services = this.services;
                                return services != null ? registrationToGetServiceFor.getType().cast( services.get( registrationToGetServiceFor ) ) : null;
                            } )
                    );

            Map<ServiceRegistrationImpl, Object> services = this.services;
            List<ServiceListenerAdapter> listeners = this.listeners;
            if( services != null && listeners != null )
            {
                services.put( registration, service );
                LOG.trace( "Registered {}", registration );

                // copying 'listeners' into a new list, because a listener might spawn an action that will cause it to
                // change while we are still iterating
                for( ServiceListenerAdapter listenerAdapter : new LinkedList<>( listeners ) )
                {
                    listenerAdapter.serviceRegistered( registration );
                }
            }

            return registration;
        } );
    }

    @Nonnull
    private <ServiceType> ServiceListenerRegistration<ServiceType> addListenerEntry( @Nonnull ServiceListenerAdapter<ServiceType> listenerAdapter )
    {
        this.lock.write( () -> {
            List<ServiceListenerAdapter> listeners = this.listeners;
            Map<ServiceRegistrationImpl, Object> services = this.services;
            if( listeners != null && services != null )
            {
                listeners.add( listenerAdapter );

                // copying 'services' into a new list, because a listener might spawn an action that will cause it to
                // change while we are still iterating
                new LinkedList<>( services.keySet() ).forEach( listenerAdapter::serviceRegistered );
            }
        } );
        return listenerAdapter;
    }

    private abstract class ServiceListenerAdapter<ServiceType>
            implements ServiceListenerRegistration<ServiceType>, ServiceListener<ServiceType>
    {
        @Nullable
        private final ModuleRevision moduleRevision;

        @Nonnull
        private final Class<ServiceType> serviceType;

        @Nullable
        private final Filter filter;

        private ServiceListenerAdapter( @Nullable ModuleRevision moduleRevision,
                                        @Nonnull Class<ServiceType> serviceType,
                                        @Nullable Filter filter )
        {
            this.moduleRevision = moduleRevision;
            this.serviceType = serviceType;
            this.filter = filter;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", getType().getName() )
                                 .add( "filter", getFilter() )
                                 .add( "listener", getListenerInstance() )
                                 .toString();
        }

        @Nullable
        @Override
        public ModuleRevision getModuleRevision()
        {
            return ServiceManagerImpl.this.lock.read( () -> this.moduleRevision );
        }

        @Nonnull
        @Override
        public Class<ServiceType> getType()
        {
            return ServiceManagerImpl.this.lock.read( () -> this.serviceType );
        }

        @Nullable
        @Override
        public String getFilter()
        {
            return ServiceManagerImpl.this.lock.read( () -> this.filter == null ? null : this.filter.toString() );
        }

        @Override
        public void unregister()
        {
            ServiceManagerImpl.this.lock.write( () -> {
                List<ServiceListenerAdapter> listeners = ServiceManagerImpl.this.listeners;
                if( listeners != null )
                {
                    listeners.remove( this );
                    LOG.trace( "Unregistered {}", this );
                }
            } );
        }

        @Override
        public final void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            ServiceListener<ServiceType> listener = getListenerInstance();
            if( listener == null )
            {
                unregister();
            }
            else if( this.serviceType.isAssignableFrom( registration.getType() ) )
            {
                if( this.filter == null || this.filter.matches( registration.getProperties() ) )
                {
                    listener.serviceRegistered( registration );
                }
            }
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            ServiceListener<ServiceType> listener = getListenerInstance();
            if( listener == null )
            {
                unregister();
            }
            else if( this.serviceType.isAssignableFrom( registration.getType() ) )
            {
                if( this.filter == null || this.filter.matches( registration.getProperties() ) )
                {
                    listener.serviceUnregistered( registration, service );
                }
            }
        }

        @Nullable
        protected abstract ServiceListener<ServiceType> getListenerInstance();
    }
}
