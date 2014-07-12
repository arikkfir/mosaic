package org.mosaic.core.services.impl;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;

/**
 * @author arik
 */
public class ServiceRegistrationImpl<ServiceType> implements ServiceRegistration<ServiceType>
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nullable
    private final ModuleRevision provider;

    @Nonnull
    private final Class<ServiceType> serviceType;

    @Nonnull
    private final Map<String, Object> properties;

    @Nonnull
    private final Consumer<ServiceRegistrationImpl<ServiceType>> unregisterAction;

    @Nonnull
    private final Function<ServiceRegistrationImpl<ServiceType>, ServiceType> getServiceAction;

    ServiceRegistrationImpl( @Nonnull ReadWriteLock lock,
                             @Nullable ModuleRevision provider,
                             @Nonnull Class<ServiceType> serviceType,
                             @Nonnull Map<String, Object> properties,
                             @Nonnull Consumer<ServiceRegistrationImpl<ServiceType>> unregisterAction,
                             @Nonnull Function<ServiceRegistrationImpl<ServiceType>, ServiceType> getServiceAction )
    {
        this.lock = lock;
        this.provider = provider;
        this.serviceType = serviceType;
        this.properties = properties;
        this.unregisterAction = unregisterAction;
        this.getServiceAction = getServiceAction;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "provider", getProvider() )
                             .add( "type", getType().getName() )
                             .add( "instance", getService() )
                             .toString();
    }

    @Nonnull
    @Override
    public ModuleRevision getProvider()
    {
        return this.lock.read( () -> this.provider );
    }

    @Nonnull
    @Override
    public Class<ServiceType> getType()
    {
        return this.lock.read( () -> this.serviceType );
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
        return this.getServiceAction.apply( this );
    }

    @Override
    public void unregister()
    {
        this.unregisterAction.accept( this );
    }
}
