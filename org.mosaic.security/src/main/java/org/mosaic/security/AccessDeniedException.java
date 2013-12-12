package org.mosaic.security;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class AccessDeniedException extends SecurityException
{
    public AccessDeniedException()
    {
    }

    public AccessDeniedException( String message )
    {
        super( message );
    }

    public AccessDeniedException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public AccessDeniedException( Throwable cause )
    {
        super( cause );
    }
}
