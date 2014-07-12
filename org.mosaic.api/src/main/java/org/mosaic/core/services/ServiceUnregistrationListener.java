package org.mosaic.core.services;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceUnregistrationListener<ServiceType>
{
    void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                              @Nonnull ServiceType service );
}
