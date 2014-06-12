package org.mosaic.core.services.impl;

import java.util.List;
import java.util.Map;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceListener;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.slf4j.Logger;

/**
 * @author arik
 */
class ServiceRegistrationImpl<ServiceType> implements ServiceRegistration<ServiceType>
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    @Nonnull
    private final Module provider;

    @Nonnull
    private final Class<ServiceType> type;

    @Nonnull
    private final Map<String, Object> properties;

    ServiceRegistrationImpl( @Nonnull ReadWriteLock lock,
                             @Nonnull Logger logger,
                             @Nonnull ServiceManagerImpl serviceManager,
                             @Nonnull Module provider,
                             @Nonnull Class<ServiceType> type,
                             @Nonnull Map<String, Object> properties )
    {
        this.lock = lock;
        this.logger = logger;
        this.serviceManager = serviceManager;
        this.provider = provider;
        this.type = type;
        this.properties = properties;
    }

    @Override
    public String toString()
    {
        return this.lock.read( () -> ToStringHelper.create( this )
                                                   .add( "provider", this.provider )
                                                   .add( "type", this.type.getName() )
                                                   .toString() );
    }

    @Nonnull
    @Override
    public Module getProvider()
    {
        return this.lock.read( () -> this.provider );
    }

    @Nonnull
    @Override
    public Class<ServiceType> getType()
    {
        return this.lock.read( () -> this.type );
    }

    @Nonnull
    @Override
    public Map<String, Object> getProperties()
    {
        return this.lock.read( () -> this.properties );
    }

    @Nullable
    @Override
    public ServiceType getService()
    {
        return this.lock.read( () -> {
            Map<ServiceRegistrationImpl, Object> services = this.serviceManager.getServices();
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }
            else
            {
                return this.type.cast( services.get( this ) );
            }
        } );
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unregister()
    {
        this.lock.write( () -> {
            Map<ServiceRegistrationImpl, Object> services = this.serviceManager.getServices();
            if( services == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            List<BaseServiceListenerAdapter> listeners = this.serviceManager.getListeners();
            if( listeners == null )
            {
                throw new IllegalStateException( "service manager no longer available (is server started?)" );
            }

            Object service = services.remove( this );
            if( service != null )
            {
                this.logger.trace( "Unregistered service {}", this );

                for( ServiceListener listener : listeners )
                {
                    try
                    {
                        listener.serviceUnregistered( this, service );
                    }
                    catch( Exception e )
                    {
                        this.logger.warn( "Service listener '{}' threw an exception reacting to service unregistration", listener, e );
                    }
                }
            }
        } );
    }
}
