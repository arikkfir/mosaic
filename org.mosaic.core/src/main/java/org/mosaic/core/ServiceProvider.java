package org.mosaic.core;

import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ServiceProvider<ServiceType>
{
    @Nullable
    ServiceRegistration<ServiceType> getRegistration();

    @Nullable
    ServiceType getService();
}
