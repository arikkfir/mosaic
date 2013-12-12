package org.mosaic.console.spi;

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
