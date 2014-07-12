package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public abstract class ModuleRevisionAdapter implements ModuleRevisionListener
{
    @Override
    public void revisionResolved( @Nonnull ModuleRevision module )
    {
        // no-op
    }

    @Override
    public void revisionStarted( @Nonnull ModuleRevision module )
    {
        // no-op
    }

    @Override
    public void revisionActivated( @Nonnull ModuleRevision module )
    {
        // no-op
    }

    @Override
    public void revisionDeactivated( @Nonnull ModuleRevision module )
    {
        // no-op
    }

    @Override
    public void revisionStopped( @Nonnull ModuleRevision module )
    {
        // no-op
    }

    @Override
    public void revisionUnresolved( @Nonnull ModuleRevision module )
    {
        // no-op
    }
}
