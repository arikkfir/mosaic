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

    <ServiceType> ListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                 @Nonnull ServiceRegisteredAction<ServiceType> onRegister,
                                                                 @Nonnull ServiceUnregisteredAction<ServiceType> onUnregister,
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

    interface ServiceRegisteredAction<ServiceType>
    {
        void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration );
    }

    interface ServiceUnregisteredAction<ServiceType>
    {
        void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                  @Nonnull ServiceType service );
    }
}
