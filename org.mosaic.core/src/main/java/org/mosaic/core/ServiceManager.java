package org.mosaic.core;

import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceManager
{
    <ServiceType> ListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                 @Nonnull ServiceListener<ServiceType> listener,
                                                                 @Nonnull Class<ServiceType> type,
                                                                 @Nonnull Module.ServiceProperty... properties );

    <ServiceType> ListenerRegistration<ServiceType> addWeakListener( @Nullable Module module,
                                                                     @Nonnull ServiceListener<ServiceType> listener,
                                                                     @Nonnull Class<ServiceType> type,
                                                                     @Nonnull Module.ServiceProperty... properties );

    @Nullable
    <ServiceType> ServiceRegistration<ServiceType> findService( @Nonnull Class<ServiceType> type,
                                                                @Nonnull Module.ServiceProperty... properties );

    @Nonnull
    <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Module module,
                                                                    @Nonnull Class<ServiceType> type,
                                                                    @Nonnull Module.ServiceProperty... properties );

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                    @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nonnull Module.ServiceProperty... properties );

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Module module,
                                                                    @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nullable Map<String, Object> properties );
}
