package org.mosaic.web.net;

/**
 * @author arik
 */
public class HttpNotFoundException extends HttpException
{
    public HttpNotFoundException()
    {
        this( "Resource not found" );
    }

    public HttpNotFoundException( String message )
    {
        super( message, HttpStatus.NOT_FOUND );
    }
}
