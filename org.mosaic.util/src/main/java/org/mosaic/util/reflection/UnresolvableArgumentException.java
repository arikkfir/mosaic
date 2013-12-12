package org.mosaic.util.reflection;

import javax.annotation.Nonnull;

/**
* @author arik
*/
@SuppressWarnings( "WeakerAccess" )
public class UnresolvableArgumentException extends RuntimeException
{
    @Nonnull
    private final MethodParameter parameter;

    public UnresolvableArgumentException( @Nonnull MethodParameter parameter )
    {
        this( "no parameter resolver provided a value for this parameter", parameter );
    }

    public UnresolvableArgumentException( @Nonnull String message, @Nonnull MethodParameter parameter )
    {
        super( "Could not resolve value for parameter '" + parameter + "': " + message );
        this.parameter = parameter;
    }

    public UnresolvableArgumentException( Throwable cause, @Nonnull MethodParameter parameter )
    {
        this( cause.getMessage(), cause, parameter );
    }

    public UnresolvableArgumentException( @Nonnull String message,
                                          @Nonnull Throwable cause,
                                          @Nonnull MethodParameter parameter )
    {
        super( "Could not resolve value for parameter '" + parameter + "': " + message, cause );
        this.parameter = parameter;
    }

    @Nonnull
    public MethodParameter getParameter()
    {
        return this.parameter;
    }
}
