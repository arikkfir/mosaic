package org.mosaic.shell;

/**
 * @author arik
 */
public class TerminateSessionException extends RuntimeException
{
    public TerminateSessionException( String message )
    {
        super( message );
    }

    public TerminateSessionException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
