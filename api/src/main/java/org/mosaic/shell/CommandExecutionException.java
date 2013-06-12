package org.mosaic.shell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        this( command, exitCode, cause.getMessage(), cause );
    }

    public CommandExecutionException( @Nonnull String command, int exitCode, @Nonnull String message )
    {
        this( command, exitCode, message, null );
    }

    public CommandExecutionException( @Nonnull String command,
                                      int exitCode,
                                      @Nonnull String message,
                                      @Nullable Throwable cause )
    {
        super( message, cause );
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
