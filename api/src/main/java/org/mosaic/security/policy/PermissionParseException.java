package org.mosaic.security.policy;

/**
 * @author arik
 */
public class PermissionParseException extends RuntimeException
{
    public PermissionParseException( String message )
    {
        super( message );
    }

    public PermissionParseException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
