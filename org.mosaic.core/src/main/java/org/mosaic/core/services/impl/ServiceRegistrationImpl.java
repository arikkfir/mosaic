package org.mosaic.core.services.impl;

import java.util.Map;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;

/**
 * @author arik
 */
class ServiceRegistrationImpl<ServiceType> implements ServiceRegistration<ServiceType>
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    @Nonnull
    private final Module provider;

    @Nonnull
    private final Class<ServiceType> type;

    @Nonnull
    private final Map<String, Object> properties;

    ServiceRegistrationImpl( @Nonnull ReadWriteLock lock,
                             @Nonnull ServiceManagerImpl serviceManager,
                             @Nonnull Module provider,
                             @Nonnull Class<ServiceType> type,
                             @Nonnull Map<String, Object> properties )
    {
        this.lock = lock;
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
        Object instance = this.serviceManager.getServiceInstanceFor( this );
        return instance == null ? null : this.type.cast( instance );
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unregister()
    {
        this.serviceManager.unregisterService( this );
    }
}
