package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ModuleStopException extends RuntimeException
{
    @Nonnull
    private final Module module;

    public ModuleStopException( String message, @Nonnull Module module )
    {
        super( message );
        this.module = module;
    }

    public ModuleStopException( String message, Throwable cause, @Nonnull Module module )
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
