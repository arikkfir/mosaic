package org.mosaic.security;

/**
 * @author arik
 */
public class CredentialNotFoundException extends RuntimeException
{
    public CredentialNotFoundException( String message )
    {
        super( message );
    }

    public CredentialNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
