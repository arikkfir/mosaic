package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class IllegalPermissionSyntaxException extends RuntimeException
{
    @Nonnull
    private final String permission;

    public IllegalPermissionSyntaxException( @Nonnull String permission )
    {
        this.permission = permission;
    }

    public IllegalPermissionSyntaxException( String message, @Nonnull String permission )
    {
        super( message );
        this.permission = permission;
    }

    public IllegalPermissionSyntaxException( String message, Throwable cause, @Nonnull String permission )
    {
        super( message, cause );
        this.permission = permission;
    }

    public IllegalPermissionSyntaxException( Throwable cause, @Nonnull String permission )
    {
        super( cause );
        this.permission = permission;
    }

    @Nonnull
    public final String getPermission()
    {
        return this.permission;
    }
}
