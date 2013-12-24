package org.mosaic.security;

import javax.annotation.Nullable;

/**
 * @author arik
 */
public class AccessDeniedException extends SecurityException
{
    @Nullable
    private final String authenticationMethod;

    public AccessDeniedException( @Nullable String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod;
    }

    public AccessDeniedException( @Nullable String message, @Nullable String authenticationMethod )
    {
        super( message );
        this.authenticationMethod = authenticationMethod;
    }

    public AccessDeniedException( @Nullable String message,
                                  @Nullable Throwable cause,
                                  @Nullable String authenticationMethod )
    {
        super( message, cause );
        this.authenticationMethod = authenticationMethod;
    }

    public AccessDeniedException( @Nullable Throwable cause, @Nullable String authenticationMethod )
    {
        super( cause );
        this.authenticationMethod = authenticationMethod;
    }

    @Nullable
    public String getAuthenticationMethod()
    {
        return this.authenticationMethod;
    }
}
