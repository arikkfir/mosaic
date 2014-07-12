package org.mosaic.core.modules;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.version.Version;

/**
 * @author arik
 */
public interface ModuleRevision
{
    @Nonnull
    Module getModule();

    long getId();

    @Nonnull
    String getName();

    @Nonnull
    Version getVersion();

    @Nonnull
    Map<String, String> getHeaders();

    boolean isCurrent();

    @Nullable
    ClassLoader getClassLoader();

    @Nullable
    Path findResource( @Nonnull String glob ) throws IOException;

    @Nonnull
    Collection<Path> findResources( @Nonnull String glob ) throws IOException;

    @Nullable
    ModuleType getType( @Nonnull Class<?> type );

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nonnull ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                                                               @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addWeakServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                   @Nonnull Class<ServiceType> type,
                                                                                   @Nonnull ServiceProperty... properties );
}
