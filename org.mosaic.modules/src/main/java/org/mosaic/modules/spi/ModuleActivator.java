package org.mosaic.modules.spi;

import org.mosaic.core.util.Nonnull;
import org.mosaic.modules.Module;

/**
 * @author arik
 */
public interface ModuleActivator
{
    void onBeforeActivate( @Nonnull Module module );

    void onAfterDeactivate( @Nonnull Module module );
}
