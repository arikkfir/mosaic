package org.mosaic.console.impl.commands;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.console.Command;
import org.mosaic.console.CommandManager;
import org.mosaic.console.Console;
import org.mosaic.console.QuitSessionException;
import org.mosaic.console.util.table.SimpleColumn;
import org.mosaic.console.util.table.TablePrinter;
import org.mosaic.modules.Component;

/**
 * @author arik
 */
@Component
final class SessionCommands
{
    @Nonnull
    @Component
    private CommandManager commandManager;

    @Command( synopsis = "show available commands, or show help for a specific command",
              description = "This command, when given no arguments, will display a list of all available commands. " +
                            "When given a name of an existing command, it will show detailed help for that command." )
    void help( @Nonnull
               Console console,

               @Nullable
               @Command.Arg( name = "command", synopsis = "name of command to get help for" )
               String commandName ) throws IOException
    {
        if( commandName == null )
        {
            @SuppressWarnings( "unchecked" )
            TablePrinter<CommandManager.CommandDescriptor> commands = new TablePrinter<>(
                    console,
                    new SimpleColumn<CommandManager.CommandDescriptor>( "Names", 15 )
                    {
                        @Nullable
                        @Override
                        public String getValue( @Nonnull CommandManager.CommandDescriptor command )
                        {
                            String names = command.getNames().toString();
                            return names.substring( 1, names.length() - 1 );
                        }
                    },
                    new SimpleColumn<CommandManager.CommandDescriptor>( "Synopsis" )
                    {
                        @Nullable
                        @Override
                        public String getValue( @Nonnull CommandManager.CommandDescriptor commandDescriptor )
                        {
                            return commandDescriptor.getSynonpsis();
                        }
                    }
            );
            commands.noChrome();

            for( CommandManager.CommandDescriptor command : this.commandManager.getCommands() )
            {
                commands.print( command );
            }
            commands.endTable();
        }
        else
        {
            this.commandManager.showHelp( console, commandName );
        }
    }

    @Command( names = { "quit", "exit" },
              synopsis = "quit from current session",
              description = "This command will end the current shell session." )
    void quit() throws IOException
    {
        throw new QuitSessionException();
    }
}
