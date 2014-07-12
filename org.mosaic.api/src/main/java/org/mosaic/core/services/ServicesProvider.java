package org.mosaic.core.services;

import java.util.List;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServicesProvider<ServiceType>
{
    @Nonnull
    List<ServiceRegistration<ServiceType>> getRegistrations();

    @Nonnull
    List<ServiceType> getServices();
}
