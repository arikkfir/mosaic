package org.mosaic.core.impl.service;

import java.util.Iterator;
import java.util.List;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Filter;

/**
 * @author arik
 */
abstract class BaseServiceListenerAdapter<ServiceType> implements ServiceListener<ServiceType>
{
    @Nonnull
    protected final Class<ServiceType> type;

    @Nullable
    protected final Filter filter;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    BaseServiceListenerAdapter( @Nonnull ReadWriteLock lock,
                                @Nonnull ServiceManagerImpl serviceManager,
                                @Nonnull Class<ServiceType> type,
                                @Nullable Filter filter )
    {
        this.lock = lock;
        this.serviceManager = serviceManager;
        this.type = type;
        this.filter = filter;
    }

    @Override
    public final void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        this.lock.acquireReadLock();
        try
        {
            ServiceListener<ServiceType> listener = getListener();
            if( listener == null )
            {
                this.lock.releaseReadLock();
                try
                {
                    unregister();
                }
                finally
                {
                    this.lock.acquireReadLock();
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
            this.lock.releaseReadLock();
        }
    }

    @Override
    public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                     @Nonnull ServiceType service )
    {
        this.lock.acquireReadLock();
        try
        {
            ServiceListener<ServiceType> listener = getListener();
            if( listener == null )
            {
                // our listener instance was garbage collected - remove ourselves from the listeners list
                this.lock.releaseReadLock();
                try
                {
                    // unregister will acquire writelock for us
                    unregister();
                }
                finally
                {
                    this.lock.acquireReadLock();
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
            this.lock.releaseReadLock();
        }
    }

    final void unregister()
    {
        this.lock.acquireWriteLock();
        try
        {
            List<BaseServiceListenerAdapter> listeners = this.serviceManager.getServiceListeners();
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
            this.lock.releaseWriteLock();
        }
    }

    @Nullable
    protected abstract ServiceListener<ServiceType> getListener();
}
