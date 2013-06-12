package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class IllegalUsageException extends CommandExecutionException
{
    public IllegalUsageException( @Nonnull String command, String message )
    {
        super( command, Command.ILLEGAL_USAGE, "Illegal usage for command '" + command + "': " + message );
    }
}
