package org.mosaic.core.impl.service;

import org.mosaic.core.ServiceListener;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Filter;

/**
 * @author arik
 */
class ServiceListenerAdapter<ServiceType> extends BaseServiceListenerAdapter<ServiceType>
{
    @Nonnull
    private final ServiceListener<ServiceType> listener;

    ServiceListenerAdapter( @Nonnull ReadWriteLock lock,
                            @Nonnull ServiceManagerImpl serviceManager,
                            @Nonnull ServiceListener<ServiceType> listener,
                            @Nonnull Class<ServiceType> type,
                            @Nullable Filter filter )
    {
        super( lock, serviceManager, type, filter );
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
