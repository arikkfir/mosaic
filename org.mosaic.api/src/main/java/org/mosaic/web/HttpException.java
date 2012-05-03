package org.mosaic.web;

/**
 * @author arik
 */
public class HttpException extends Exception
{
    public HttpException( String message )
    {
        super( message );
    }

    public HttpException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
