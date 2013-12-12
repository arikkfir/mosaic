package org.mosaic.console.spi;

/**
 * @author arik
 */
public class CommandNotFoundException extends CommandExecutionException
{
    public CommandNotFoundException( String message )
    {
        super( message );
    }
}
