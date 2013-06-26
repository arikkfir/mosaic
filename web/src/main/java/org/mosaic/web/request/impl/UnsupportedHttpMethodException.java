package org.mosaic.web.request.impl;

/**
 * @author arik
 */
public class UnsupportedHttpMethodException extends Exception
{
    public UnsupportedHttpMethodException( String message )
    {
        super( message );
    }

    public UnsupportedHttpMethodException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
