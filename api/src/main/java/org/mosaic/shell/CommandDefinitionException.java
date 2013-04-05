package org.mosaic.shell;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class CommandDefinitionException extends Exception
{
    @Nonnull
    private final String commandName;

    public CommandDefinitionException( String message, @Nonnull String commandName )
    {
        super( "Shell command definition error for '" + commandName + "': " + message );
        this.commandName = commandName;
    }

    public CommandDefinitionException( String message, Throwable cause, @Nonnull String commandName )
    {
        super( "Shell command definition error for '" + commandName + "': " + message, cause );
        this.commandName = commandName;
    }

    @Nonnull
    public String getCommandName()
    {
        return commandName;
    }
}
