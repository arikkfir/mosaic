package org.mosaic.core.impl.service;

import java.lang.ref.WeakReference;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Filter;

/**
 * @author arik
 */
class WeakServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
{
    @Nonnull
    private final WeakReference<ServiceListener<ServiceType>> listener;

    WeakServiceListenerAdapter( @Nonnull ReadWriteLock lock,
                                @Nonnull ServiceManagerImpl serviceManager,
                                @Nonnull ServiceListener<ServiceType> listener,
                                @Nonnull Class<ServiceType> type,
                                @Nullable Filter filter )
    {
        super( lock, serviceManager, type, filter );
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
