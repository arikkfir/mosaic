package org.mosaic.core.services;

import java.util.Map;
import org.mosaic.core.modules.Module;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceManager
{
    <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                        @Nonnull ServiceListener<ServiceType> listener,
                                                                        @Nonnull Class<ServiceType> type,
                                                                        @Nonnull Module.ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addListener( @Nullable Module module,
                                                                        @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                                                        @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                        @Nonnull Class<ServiceType> type,
                                                                        @Nonnull Module.ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addWeakListener( @Nullable Module module,
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
