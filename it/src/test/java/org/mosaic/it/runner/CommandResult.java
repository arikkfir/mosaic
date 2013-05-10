package org.mosaic.it.runner;

import javax.annotation.Nonnull;

public class CommandResult
{
    private final int exitCode;

    @Nonnull
    private final String commandLine;

    @Nonnull
    private final String output;

    public CommandResult( int exitCode, @Nonnull String commandLine, @Nonnull String output )
    {
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

    public CommandResult assertSuccess()
    {
        if( this.exitCode != 0 )
        {
            throw new CommandFailedException( "Command failed: " + commandLine + "\n" +
                                              "==================================================================\n" +
                                              this.output + "\n" +
                                              "==================================================================\n",
                                              this.exitCode,
                                              this.commandLine,
                                              this.output );
        }
        return this;
    }
}
