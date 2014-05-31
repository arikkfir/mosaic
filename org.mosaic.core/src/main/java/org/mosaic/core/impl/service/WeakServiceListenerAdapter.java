package org.mosaic.core.impl.service;

import java.lang.ref.WeakReference;
import org.mosaic.core.Module;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.slf4j.Logger;

/**
 * @author arik
 */
class WeakServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
{
    @Nonnull
    private final WeakReference<ServiceListener<ServiceType>> listener;

    WeakServiceListenerAdapter( @Nonnull Logger logger,
                                @Nonnull ReadWriteLock lock,
                                @Nonnull ServiceManagerImpl serviceManager,
                                @Nonnull ServiceListener<ServiceType> listener,
                                @Nonnull Class<ServiceType> type,
                                @Nonnull Module.ServiceProperty... properties )
    {
        super( logger, lock, serviceManager, type, properties );
        this.listener = new WeakReference<>( listener );
    }

    @Nullable
    @Override
    protected ServiceListener<ServiceType> getListener()
    {
        return this.listener.get();
    }
}
