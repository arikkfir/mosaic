package org.mosaic.web.application;

/**
 * @author arik
 */
public class WebApplicationParseException extends Exception
{
    public WebApplicationParseException( String message )
    {
        super( message );
    }

    public WebApplicationParseException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
