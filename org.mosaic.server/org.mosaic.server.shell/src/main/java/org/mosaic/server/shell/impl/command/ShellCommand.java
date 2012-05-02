package org.mosaic.server.shell.impl.command;

import java.io.IOException;
import org.mosaic.server.shell.console.Console;

/**
 * @author arik
 */
public interface ShellCommand
{

    String getName();

    void execute( Console console, String... args ) throws Exception;

    String getOrigin();

    String getDescription();

    String getAdditionalArgumentsDescription();

    void showHelp( Console console ) throws IOException;
}
