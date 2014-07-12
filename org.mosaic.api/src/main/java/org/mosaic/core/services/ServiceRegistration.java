package org.mosaic.core.services;

import java.util.Map;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceRegistration<ServiceType>
{
    @Nullable
    ModuleRevision getProvider();

    @Nonnull
    Class<ServiceType> getType();

    @Nonnull
    Map<String, Object> getProperties();

    @Nullable
    ServiceType getService();

    void unregister();
}
