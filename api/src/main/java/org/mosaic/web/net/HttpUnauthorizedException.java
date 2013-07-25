package org.mosaic.web.net;

/**
 * @author arik
 */
public class HttpUnauthorizedException extends HttpException
{
    public HttpUnauthorizedException()
    {
        super( "Bad credentials", HttpStatus.UNAUTHORIZED );
    }
}
