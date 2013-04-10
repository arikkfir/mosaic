package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class CommandExecutionException extends Exception
{
    private final int exitCode;

    @Nonnull
    private final String command;

    public CommandExecutionException( @Nonnull String command, int exitCode, @Nonnull Throwable cause )
    {
        super( cause.getMessage(), cause );
        this.exitCode = exitCode;
        this.command = command;
    }

    public int getExitCode()
    {
        return exitCode;
    }

    @Nonnull
    public String getCommand()
    {
        return command;
    }
}
