package org.mosaic.console.spi;

import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.console.Console;

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
        String getSynonpsis();

        @Nullable
        String getDescription();
    }
}
