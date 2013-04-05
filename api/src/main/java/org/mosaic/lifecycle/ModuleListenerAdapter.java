package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

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
    public void moduleActivated( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleDeactivated( @Nonnull Module module )
    {
        // no-op
    }

    @Override
    public void moduleUninstalled( @Nonnull Module module )
    {
        // no-op
    }
}
