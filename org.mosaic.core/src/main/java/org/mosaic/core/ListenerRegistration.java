package org.mosaic.core;

import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ListenerRegistration<ServiceType>
{
    @Nullable
    Module getModule();

    @Nonnull
    Class<ServiceType> getType();

    @Nonnull
    Map<String, Object> getProperties();

    void unregister();
}
