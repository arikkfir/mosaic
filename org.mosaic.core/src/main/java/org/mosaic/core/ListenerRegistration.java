package org.mosaic.core;

import java.util.Map;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ListenerRegistration<ServiceType>
{
    @Nonnull
    Class<ServiceType> getType();

    @Nonnull
    Map<String, Object> getProperties();

    void unregister();
}
