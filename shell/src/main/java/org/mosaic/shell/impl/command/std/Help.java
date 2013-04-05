package org.mosaic.shell.impl.command.std;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.shell.CommandDefinitionException;
import org.mosaic.shell.Console;
import org.mosaic.shell.annotation.Arguments;
import org.mosaic.shell.annotation.Command;
import org.mosaic.shell.impl.command.CommandExecutor;
import org.mosaic.shell.impl.command.CommandManager;

/**
 * @author arik
 */
@Bean
public class Help
{
    private CommandManager commandManager;

    @BeanRef
    public void setCommandManager( CommandManager commandManager )
    {
        this.commandManager = commandManager;
    }

    @Command(label = "Help", desc = "Shows help for specific commands or shows the list of available commands")
    public void help( @Nonnull Console console, @Nonnull @Arguments String... commandNames )
            throws IOException, CommandDefinitionException
    {
        if( commandNames.length > 0 )
        {
            for( String commandName : commandNames )
            {
                CommandExecutor commandExecutor = this.commandManager.getCommand( commandName );
                if( commandExecutor != null )
                {
                    commandExecutor.printHelp( console );
                }
            }
        }
        else
        {
            console.println();
            console.println( "Following commands are available:" );
            console.println();

            Console.TableHeaders table = console.createTable( 2 );
            table.addHeader( "Name", 0.2 );
            table.addHeader( "Label", 0.2 );
            table.addHeader( "Description" );

            Console.TablePrinter tablePrinter = table.start();
            for( CommandExecutor commandExecutor : this.commandManager.getCommandExecutors() )
            {
                org.mosaic.shell.Command command = commandExecutor.getCommand();
                tablePrinter.print( command.getName(), command.getLabel(), command.getDescription() );
            }
            tablePrinter.done();

            console.println();
        }
    }
}
