package org.mosaic.server.shell.commands.impl;

import java.io.PrintWriter;
import org.mosaic.server.shell.Option;
import org.mosaic.server.shell.ShellCommand;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class Commands {

    @ShellCommand( "list" )
    public void listBundles(

            PrintWriter writer,

            @Option( alias = "t" )
            Boolean test

    ) {
        writer.println( "Testing! command (test=" + test + ")" );
    }

}
