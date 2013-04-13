package org.mosaic.security;

/**
 * @author arik
 */
public class PrincipalNotFoundException extends RuntimeException
{
    public PrincipalNotFoundException( String message )
    {
        super( message );
    }

    public PrincipalNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
