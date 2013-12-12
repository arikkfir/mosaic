package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class AuthorizationException extends SecurityException
{
    @Nonnull
    private final Subject subject;

    @Nonnull
    private final Permission permission;

    public AuthorizationException( @Nonnull Subject subject, @Nonnull Permission permission )
    {
        this.subject = subject;
        this.permission = permission;
    }

    public AuthorizationException( String message,
                                   @Nonnull Subject subject,
                                   @Nonnull Permission permission )
    {
        super( message );
        this.subject = subject;
        this.permission = permission;
    }

    public AuthorizationException( String message,
                                   Throwable cause,
                                   @Nonnull Subject subject,
                                   @Nonnull Permission permission )
    {
        super( message, cause );
        this.subject = subject;
        this.permission = permission;
    }

    public AuthorizationException( Throwable cause,
                                   @Nonnull Subject subject,
                                   @Nonnull Permission permission )
    {
        super( cause );
        this.subject = subject;
        this.permission = permission;
    }

    @Nonnull
    public final Subject getSubject()
    {
        return this.subject;
    }

    @Nonnull
    public final Permission getPermission()
    {
        return this.permission;
    }
}
