package org.mosaic.console;

/**
 * @author arik
 */
public class BadUsageException extends CommandExecutionException
{
    public BadUsageException( String message )
    {
        super( message );
    }
}
