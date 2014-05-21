package org.mosaic.core.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
abstract class ModuleRevisionImplDependency
{
    @Nonnull
    protected final ModuleRevisionImpl moduleRevision;

    ModuleRevisionImplDependency( @Nonnull ModuleRevisionImpl moduleRevision )
    {
        this.moduleRevision = moduleRevision;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "revision", this.moduleRevision )
                             .toString();
    }

    final void notifySatisfaction()
    {
        this.moduleRevision.notifySatisfaction( this, true );
    }

    final void notifyUnsatisfaction()
    {
        this.moduleRevision.notifySatisfaction( this, false );
    }

    void initialize()
    {
        // no-op
    }

    void shutdown()
    {
        // no-op
    }
}
