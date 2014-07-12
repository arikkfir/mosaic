package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ModuleRevisionListener
{
    void revisionResolved( @Nonnull ModuleRevision module );

    void revisionStarted( @Nonnull ModuleRevision module );

    void revisionActivated( @Nonnull ModuleRevision module );

    void revisionDeactivated( @Nonnull ModuleRevision module );

    void revisionStopped( @Nonnull ModuleRevision module );

    void revisionUnresolved( @Nonnull ModuleRevision module );
}
