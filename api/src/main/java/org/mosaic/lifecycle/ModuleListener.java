package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ModuleListener
{
    void moduleInstalled( @Nonnull Module module );

    void moduleActivated( @Nonnull Module module );

    void moduleDeactivated( @Nonnull Module module );

    void moduleUninstalled( @Nonnull Module module );
}
