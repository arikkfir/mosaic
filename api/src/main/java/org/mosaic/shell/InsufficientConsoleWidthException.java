package org.mosaic.shell;

/**
 * @author arik
 */
public class InsufficientConsoleWidthException extends RuntimeException
{
    public InsufficientConsoleWidthException( String message )
    {
        super( message );
    }
}
