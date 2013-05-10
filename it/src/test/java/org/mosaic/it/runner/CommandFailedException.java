package org.mosaic.it.runner;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class CommandFailedException extends RuntimeException
{
    private final int exitCode;

    @Nonnull
    private final String commandLine;

    @Nonnull
    private final String output;

    public CommandFailedException( @Nonnull String message,
                                   int exitCode,
                                   @Nonnull String commandLine,
                                   @Nonnull String output )
    {
        super( message );
        this.exitCode = exitCode;
        this.commandLine = commandLine;
        this.output = output;
    }

    public CommandFailedException( @Nonnull String message,
                                   @Nonnull Throwable cause,
                                   int exitCode,
                                   @Nonnull String commandLine,
                                   @Nonnull String output )
    {
        super( message, cause );
        this.exitCode = exitCode;
        this.commandLine = commandLine;
        this.output = output;
    }

    public int getExitCode()
    {
        return exitCode;
    }

    @Nonnull
    public String getCommandLine()
    {
        return commandLine;
    }

    @Nonnull
    public String getOutput()
    {
        return output;
    }
}
