package org.mosaic.console.remote.impl;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.mosaic.console.Console;

/**
 * @author arik
 */
final class SingleCommand extends AbstractSshdCommand
{
    static final class Factory implements CommandFactory
    {
        @Override
        public Command createCommand( @Nonnull String command )
        {
            return new SingleCommand( command );
        }
    }

    @Nonnull
    private final String command;

    private SingleCommand( @Nonnull String command )
    {
        this.command = command;
    }

    @Override
    protected void execute( @Nonnull Console console ) throws IOException
    {
        processInputLine( console, this.command );
    }
}
