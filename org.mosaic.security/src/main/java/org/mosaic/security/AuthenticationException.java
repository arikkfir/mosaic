package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class AuthenticationException extends SecurityException
{
    @Nonnull
    private final AuthenticationToken authenticationToken;

    public AuthenticationException( @Nonnull AuthenticationToken authenticationToken )
    {
        this.authenticationToken = authenticationToken;
    }

    public AuthenticationException( String message,
                                    @Nonnull AuthenticationToken authenticationToken )
    {
        super( message );
        this.authenticationToken = authenticationToken;
    }

    public AuthenticationException( String message,
                                    Throwable cause,
                                    @Nonnull AuthenticationToken authenticationToken )
    {
        super( message, cause );
        this.authenticationToken = authenticationToken;
    }

    public AuthenticationException( Throwable cause,
                                    @Nonnull AuthenticationToken authenticationToken )
    {
        super( cause );
        this.authenticationToken = authenticationToken;
    }

    @Nonnull
    public final AuthenticationToken getAuthenticationToken()
    {
        return authenticationToken;
    }
}
