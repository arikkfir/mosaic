package org.mosaic.core;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ModuleStartException extends RuntimeException
{
    @Nonnull
    private final Module module;

    public ModuleStartException( String message, @Nonnull Module module )
    {
        super( message );
        this.module = module;
    }

    public ModuleStartException( String message, Throwable cause, @Nonnull Module module )
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
