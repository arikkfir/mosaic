package org.mosaic.server.shell.commands.impl;

import java.io.IOException;
import org.mosaic.describe.Description;
import org.mosaic.server.shell.ExitSessionException;
import org.mosaic.server.shell.ShellCommand;
import org.mosaic.server.shell.console.Console;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class ExitCommand extends AbstractCommand {

    @Description( "Exit current session" )
    @ShellCommand( "exit" )
    public void exit( Console console ) throws IOException {
        throw new ExitSessionException();
    }

}
