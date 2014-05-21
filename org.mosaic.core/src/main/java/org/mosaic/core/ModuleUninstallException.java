package org.mosaic.core;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ModuleUninstallException extends RuntimeException
{
    @Nonnull
    private final Module module;

    public ModuleUninstallException( String message, @Nonnull Module module )
    {
        super( message );
        this.module = module;
    }

    public ModuleUninstallException( String message, Throwable cause, @Nonnull Module module )
    {
        super( message, cause );
        this.module = module;
    }

    @Nonnull
    public Module getModule()
    {
        return this.module;
    }
}
