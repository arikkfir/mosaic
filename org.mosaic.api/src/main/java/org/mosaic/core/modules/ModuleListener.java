package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ModuleListener
{
    void moduleInstalled( @Nonnull Module module );

    void moduleResolved( @Nonnull Module module );

    void moduleStarted( @Nonnull Module module );

    void moduleStopped( @Nonnull Module module );

    void moduleUnresolved( @Nonnull Module module );

    void moduleUpdated( @Nonnull Module module );

    void moduleUninstalled( @Nonnull Module module );
}
