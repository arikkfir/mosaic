package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class IllegalUsageException extends Exception
{
    @Nonnull
    private final String command;

    public IllegalUsageException( @Nonnull String command, String message )
    {
        super( "Illegal usage for command '" + command + "': " + message );
        this.command = command;
    }

    @Nonnull
    public String getCommand()
    {
        return command;
    }
}
