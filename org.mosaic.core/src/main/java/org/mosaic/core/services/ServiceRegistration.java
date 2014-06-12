package org.mosaic.core.services;

import java.util.Map;
import org.mosaic.core.modules.Module;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceRegistration<ServiceType>
{
    @Nonnull
    Module getProvider();

    @Nonnull
    Class<ServiceType> getType();

    @Nonnull
    Map<String, Object> getProperties();

    @Nullable
    ServiceType getService();

    void unregister();
}
