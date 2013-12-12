package org.mosaic.security;

/**
 * @author arik
 */
public abstract class SecurityException extends RuntimeException
{
    protected SecurityException()
    {
    }

    protected SecurityException( String message )
    {
        super( message );
    }

    protected SecurityException( String message, Throwable cause )
    {
        super( message, cause );
    }

    protected SecurityException( Throwable cause )
    {
        super( cause );
    }
}
