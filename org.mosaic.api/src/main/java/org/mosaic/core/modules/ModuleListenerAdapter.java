package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public abstract class ModuleListenerAdapter implements ModuleListener
{
    @Override
    public void moduleInstalled( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleResolved( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleStarted( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleStopped( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleUnresolved( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleUpdated( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleUninstalled( @Nonnull Module module )
    {
        // no-op
    }
}
