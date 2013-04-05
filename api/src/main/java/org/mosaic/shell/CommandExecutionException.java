package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class CommandExecutionException extends Exception
{
    @Nonnull
    private final String command;

    public CommandExecutionException( @Nonnull String command, @Nonnull Throwable cause )
    {
        super( cause.getMessage(), cause );
        this.command = command;
    }

    @Nonnull
    public String getCommand()
    {
        return command;
    }
}
