package org.mosaic.console.spi;

/**
 * @author arik
 */
public class CommandExecutionException extends RuntimeException
{
    public CommandExecutionException( String message )
    {
        super( message );
    }

    public CommandExecutionException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
