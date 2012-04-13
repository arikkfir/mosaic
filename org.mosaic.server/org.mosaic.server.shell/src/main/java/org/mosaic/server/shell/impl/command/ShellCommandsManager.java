package org.mosaic.server.shell.impl.command;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.shell.Console;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ShellCommandsManager {

    private final Map<MethodEndpointInfo, ShellCommand> commands = new ConcurrentHashMap<>();

    private final HelpCommand helpCommand = new HelpCommand();

    @ServiceBind
    public synchronized void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( org.mosaic.server.shell.ShellCommand.class ) ) {
            this.commands.put( methodEndpointInfo, new MethodEndpointShellCommand( methodEndpointInfo ) );
        }
    }

    @ServiceUnbind
    public synchronized void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( org.mosaic.server.shell.ShellCommand.class ) ) {
            this.commands.remove( methodEndpointInfo );
        }
    }

    public ShellCommand getCommand( String commandName ) {
        if( "help".equalsIgnoreCase( commandName ) ) {
            return this.helpCommand;
        }

        for( ShellCommand shellCommandImpl : this.commands.values() ) {
            if( shellCommandImpl.getName().equals( commandName ) ) {
                return shellCommandImpl;
            }
        }

        return null;
    }

    public Collection<ShellCommand> getCommands() {
        List<ShellCommand> commands = new ArrayList<>( this.commands.values() );
        commands.add( this.helpCommand );
        Collections.sort( commands, new CommandComparator() );
        return commands;
    }

    private class HelpCommand implements ShellCommand {

        @Override
        public String getName() {
            return "help";
        }

        @Override
        public void execute( Console console, String... args ) throws Exception {
            if( args.length == 0 ) {

                console.println( "Following commands are available:" );
                Console.TablePrinter table = console.createTable()
                                                    .addHeader( "Command", 10 )
                                                    .addHeader( "Description", 50 )
                                                    .addHeader( "Origin", 40 )
                                                    .start();
                for( ShellCommand command : getCommands() ) {
                    table.print( command.getName(), command.getDescription(), command.getOrigin() );
                }
                table.done();

            } else {

                for( String commandName : args ) {
                    ShellCommand command = getCommand( commandName );
                    if( command == null ) {
                        console.println( "Command '" + commandName + "' could not be found" );
                    } else {
                        command.showHelp( console );
                    }
                }

            }
        }

        @Override
        public String getAdditionalArgumentsDescription() {
            return "command-names";
        }

        @Override
        public String getOrigin() {
            return "Built-in";
        }

        @Override
        public String getDescription() {
            return "Shows help about currently available commands";
        }

        @Override
        public void showHelp( Console console ) throws IOException {
            console.println( "Shows currently available commands, or, when given command name(s)," );
            console.println( "prints specific help about these commands." );
            console.println( "Syntax: help [command-name]*" );
        }

    }

    private static class CommandComparator implements Comparator<ShellCommand> {

        @Override
        public int compare( ShellCommand o1, ShellCommand o2 ) {
            return o1.getName().compareTo( o2.getName() );
        }
    }
}
