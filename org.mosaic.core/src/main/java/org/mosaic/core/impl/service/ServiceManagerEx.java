package org.mosaic.core.impl.service;

import org.mosaic.core.Module;
import org.mosaic.core.ServiceManager;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ServiceManagerEx extends ServiceManager
{
    void unregisterServicesFrom( @Nonnull Module module );

    void unregisterListenersFrom( @Nonnull Module module );
}
