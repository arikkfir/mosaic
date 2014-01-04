package org.mosaic.web.request;

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
