package org.mosaic.core.services;

import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceListenerRegistration<ServiceType>
{
    @Nullable
    ModuleRevision getModuleRevision();

    @Nonnull
    Class<ServiceType> getType();

    @Nullable
    String getFilter();

    void unregister();
}
