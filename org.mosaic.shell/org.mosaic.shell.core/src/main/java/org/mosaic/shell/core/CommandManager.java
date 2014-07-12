package org.mosaic.shell.core;

import java.io.IOException;
import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface CommandManager
{
    @Nonnull
    Collection<CommandDescriptor> getCommands();

    void execute( @Nonnull Console console, @Nonnull String commandLine ) throws IOException;

    void showHelp( @Nonnull Console console, @Nonnull String commandName ) throws IOException;

    interface CommandDescriptor
    {
        @Nonnull
        Collection<String> getNames();

        @Nullable
        String getSynopsis();

        @Nullable
        String getDescription();
    }
}
