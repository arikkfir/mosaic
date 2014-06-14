package org.mosaic.core.services;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceTracker<ServiceType> extends ServicesProvider<ServiceType>
{
    void addEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void addEventHandler( @Nullable ServiceRegistrationListener<ServiceType> onRegister,
                          @Nullable ServiceUnregistrationListener<ServiceType> onUnregister );

    void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener );

    void startTracking();

    void stopTracking();
}
