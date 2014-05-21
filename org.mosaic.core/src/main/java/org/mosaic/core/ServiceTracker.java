package org.mosaic.core;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceTracker<ServiceType> extends ServicesProvider<ServiceType>
{
    void addEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void startTracking();

    void stopTracking();
}
