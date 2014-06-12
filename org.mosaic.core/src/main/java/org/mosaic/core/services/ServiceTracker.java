package org.mosaic.core.services;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceTracker<ServiceType> extends ServicesProvider<ServiceType>
{
    void addEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void addEventHandler( @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                          @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister );

    void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void startTracking();

    void stopTracking();
}
