package org.mosaic.core;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceManager
{
    <ServiceType> void addListener( @Nonnull ServiceListener<ServiceType> listener,
                                    @Nonnull Class<ServiceType> type,
                                    @Nonnull Module.ServiceProperty... properties );

    <ServiceType> void addWeakListener( @Nonnull ServiceListener<ServiceType> listener,
                                        @Nonnull Class<ServiceType> type,
                                        @Nonnull Module.ServiceProperty... properties );

    void removeListener( @Nonnull ServiceListener<?> listener );

    @Nullable
    <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                @Nonnull Module.ServiceProperty... properties );

    @Nonnull
    <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                    @Nonnull Module.ServiceProperty... properties );
}
