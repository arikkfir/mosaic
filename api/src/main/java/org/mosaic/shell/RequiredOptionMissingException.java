package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class RequiredOptionMissingException extends Exception
{
    @Nonnull
    private final String command;

    @Nonnull
    private final String option;

    public RequiredOptionMissingException( @Nonnull String command, @Nonnull String option )
    {
        super( "Missing option '" + option + "' for command '" + command + "'" );
        this.command = command;
        this.option = option;
    }

    @Nonnull
    public String getCommand()
    {
        return command;
    }

    @Nonnull
    public String getOption()
    {
        return option;
    }
}
