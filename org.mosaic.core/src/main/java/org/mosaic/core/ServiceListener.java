package org.mosaic.core;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceListener<ServiceType>
{
    void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration );

    void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration, @Nonnull ServiceType service );
}
