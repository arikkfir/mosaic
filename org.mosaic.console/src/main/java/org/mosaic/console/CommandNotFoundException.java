package org.mosaic.console;

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
