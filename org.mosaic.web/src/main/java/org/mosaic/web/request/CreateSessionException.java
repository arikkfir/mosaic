package org.mosaic.web.request;

/**
 * @author arik
 */
public class CreateSessionException extends RuntimeException
{
    public CreateSessionException( String message )
    {
        super( message );
    }

    public CreateSessionException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
