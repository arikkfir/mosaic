package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public abstract class ModuleException extends RuntimeException
{
    private final Module module;

    protected ModuleException( @Nonnull String message, @Nullable Module module )
    {
        super( message );
        this.module = module;
    }

    protected ModuleException( @Nonnull String message, @Nonnull Throwable cause, @Nullable Module module )
    {
        super( message, cause );
        this.module = module;
    }

    protected ModuleException( Throwable cause, @Nullable Module module )
    {
        super( cause );
        this.module = module;
    }

    public final Module getModule()
    {
        return this.module;
    }
}
