package org.mosaic.web.server;

/**
 * @author arik
 */
public class CreateWebInvocationException extends RuntimeException
{
    public CreateWebInvocationException( String message )
    {
        super( message );
    }

    public CreateWebInvocationException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
