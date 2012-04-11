package org.mosaic.server.shell.impl.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.shell.ShellCommand;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ShellCommandsManager {

    private final Map<MethodEndpointInfo, CommandInfo> commands = new ConcurrentHashMap<>();

    @ServiceBind
    public synchronized void addListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ShellCommand.class ) ) {
            this.commands.put( methodEndpointInfo, new CommandInfo( methodEndpointInfo ) );
        }
    }

    @ServiceUnbind
    public synchronized void removeListener( MethodEndpointInfo methodEndpointInfo ) {
        if( methodEndpointInfo.isOfType( ShellCommand.class ) ) {
            this.commands.remove( methodEndpointInfo );
        }
    }

    public CommandInfo getCommand( String commandName ) {
        for( CommandInfo commandInfo : this.commands.values() ) {
            if( commandInfo.getName().equals( commandName ) ) {
                return commandInfo;
            }
        }
        return null;
    }
}
