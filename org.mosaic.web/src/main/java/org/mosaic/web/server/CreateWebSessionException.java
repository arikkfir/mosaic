package org.mosaic.web.server;

/**
 * @author arik
 */
public class CreateWebSessionException extends RuntimeException
{
    public CreateWebSessionException( String message )
    {
        super( message );
    }

    public CreateWebSessionException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
