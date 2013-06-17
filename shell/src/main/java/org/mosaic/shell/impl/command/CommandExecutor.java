package org.mosaic.shell.impl.command;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.shell.Command;
import org.mosaic.shell.CommandDefinitionException;
import org.mosaic.shell.Console;

/**
 * @author arik
 */
public interface CommandExecutor
{
    @Nonnull
    Command getCommand();

    int execute( @Nonnull Console console, @Nonnull String... arguments ) throws IOException;

    void printHelp( @Nonnull Console console ) throws CommandDefinitionException, IOException;

    void printUsage( @Nonnull Console console ) throws CommandDefinitionException;
}
