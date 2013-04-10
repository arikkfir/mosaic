package org.mosaic.shell.impl.command;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.shell.*;

/**
 * @author arik
 */
public interface CommandExecutor
{
    @Nonnull
    Command getCommand();

    int execute( @Nonnull Console console, @Nonnull String... arguments )
            throws CommandDefinitionException, IllegalUsageException, CommandExecutionException, IOException;

    void printHelp( @Nonnull Console console ) throws CommandDefinitionException, IOException;

    void printUsage( @Nonnull Console console ) throws CommandDefinitionException;
}
