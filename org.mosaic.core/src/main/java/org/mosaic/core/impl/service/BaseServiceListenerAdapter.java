package org.mosaic.core.impl.service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mosaic.core.ListenerRegistration;
import org.mosaic.core.Module;
import org.mosaic.core.ServiceListener;
import org.mosaic.core.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.osgi.framework.Filter;
import org.slf4j.Logger;

import static java.util.Collections.unmodifiableMap;
import static org.mosaic.core.impl.service.ServiceManagerImpl.createFilter;

/**
 * @author arik
 */
abstract class BaseServiceListenerAdapter<ServiceType> implements ServiceListener<ServiceType>,
                                                                  ListenerRegistration<ServiceType>
{
    @Nonnull
    protected final Class<ServiceType> type;

    @Nullable
    protected final Filter filter;

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    @Nonnull
    private final Map<String, Object> properties;

    BaseServiceListenerAdapter( @Nonnull Logger logger,
                                @Nonnull ReadWriteLock lock,
                                @Nonnull ServiceManagerImpl serviceManager,
                                @Nonnull Class<ServiceType> type,
                                @Nonnull Module.ServiceProperty... properties )
    {
        this.logger = logger;
        this.lock = lock;
        this.serviceManager = serviceManager;
        this.type = type;
        this.filter = createFilter( properties );

        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        for( Module.ServiceProperty property : properties )
        {
            propertiesMap.put( property.getName(), property.getValue() );
        }
        this.properties = unmodifiableMap( propertiesMap );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.type.getName() )
                             .add( "filter", this.filter )
                             .add( "listener", getListener() )
                             .toString();
    }

    @Nonnull
    @Override
    public Class<ServiceType> getType()
    {
        return this.type;
    }

    @Nonnull
    @Override
    public Map<String, Object> getProperties()
    {
        return this.properties;
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

    @Override
    public final void unregister()
    {
        this.lock.acquireWriteLock();
        try
        {
            List<BaseServiceListenerAdapter> listeners = this.serviceManager.getListeners();
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
                    this.logger.trace( "Removed listener entry {}", this );
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
