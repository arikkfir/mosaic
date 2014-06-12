package org.mosaic.core.services;

import java.util.Map;
import org.mosaic.core.modules.Module;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceListenerRegistration<ServiceType>
{
    @Nullable
    Module getModule();

    @Nonnull
    Class<ServiceType> getType();

    @Nonnull
    Map<String, Object> getProperties();

    void unregister();
}
