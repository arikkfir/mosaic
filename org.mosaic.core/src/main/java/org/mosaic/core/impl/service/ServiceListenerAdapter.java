package org.mosaic.core.impl.service;

import org.mosaic.core.Module;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.slf4j.Logger;

/**
 * @author arik
 */
class ServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
{
    @Nonnull
    private final ServiceListener<ServiceType> listener;

    ServiceListenerAdapter( @Nonnull Logger logger,
                            @Nonnull ReadWriteLock lock,
                            @Nonnull ServiceManagerImpl serviceManager,
                            @Nullable Module module,
                            @Nonnull ServiceListener<ServiceType> listener,
                            @Nonnull Class<ServiceType> type,
                            @Nonnull Module.ServiceProperty... properties )
    {
        super( logger, lock, serviceManager, module, type, properties );
        this.listener = listener;
    }

    @Nonnull
    @Override
    protected ServiceListener<ServiceType> getListener()
    {
        return this.listener;
    }
}
