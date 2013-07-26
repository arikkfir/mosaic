package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public abstract class ModuleBeanNotFoundException extends RuntimeException
{
    @Nonnull
    private final Module module;

    protected ModuleBeanNotFoundException( @Nonnull Module module )
    {
        this.module = module;
    }

    protected ModuleBeanNotFoundException( String message, @Nonnull Module module )
    {
        super( message );
        this.module = module;
    }

    protected ModuleBeanNotFoundException( String message, Throwable cause, @Nonnull Module module )
    {
        super( message, cause );
        this.module = module;
    }

    protected ModuleBeanNotFoundException( Throwable cause, @Nonnull Module module )
    {
        super( cause );
        this.module = module;
    }

    protected ModuleBeanNotFoundException( String message,
                                           Throwable cause,
                                           boolean enableSuppression,
                                           boolean writableStackTrace, @Nonnull Module module )
    {
        super( message, cause, enableSuppression, writableStackTrace );
        this.module = module;
    }

    @Nonnull
    public final Module getModule()
    {
        return this.module;
    }
}
