package org.mosaic.core;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceTracker<ServiceType> extends ServicesProvider<ServiceType>
{
    void addEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void addEventHandler( @Nonnull ServiceManager.ServiceRegisteredAction<ServiceType> onRegister,
                          @Nonnull ServiceManager.ServiceUnregisteredAction<ServiceType> onUnregister );

    void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void startTracking();

    void stopTracking();
}
