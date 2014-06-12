package org.mosaic.core.services;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceRegistrationListener<ServiceType>
{
    void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration );
}
