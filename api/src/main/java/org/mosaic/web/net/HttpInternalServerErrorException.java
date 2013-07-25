package org.mosaic.web.net;

/**
 * @author arik
 */
public class HttpInternalServerErrorException extends HttpException
{
    public HttpInternalServerErrorException()
    {
        this( "Internal error" );
    }

    public HttpInternalServerErrorException( String message )
    {
        super( message, HttpStatus.INTERNAL_SERVER_ERROR );
    }
}
