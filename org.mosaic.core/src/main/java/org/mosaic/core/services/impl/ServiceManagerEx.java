package org.mosaic.core.services.impl;

import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceManager;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceManagerEx extends ServiceManager
{
    void unregisterServicesFrom( @Nonnull Module module );

    void unregisterListenersFrom( @Nonnull Module module );
}
