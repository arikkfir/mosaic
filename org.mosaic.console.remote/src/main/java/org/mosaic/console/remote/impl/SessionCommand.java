package org.mosaic.console.remote.impl;

import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.mosaic.console.Console;
import org.mosaic.console.spi.CommandCanceledException;
import org.mosaic.console.spi.QuitSessionException;
import org.mosaic.modules.Service;
import org.mosaic.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class SessionCommand extends AbstractSshdCommand
{
    private static final Logger LOG = LoggerFactory.getLogger( SessionCommand.class );

    static final class Factory implements org.apache.sshd.common.Factory<Command>
    {
        @Override
        public Command create()
        {
            return new SessionCommand();
        }
    }

    @Nonnull
    @Service
    private Server server;

    @Nullable
    private FileHistory history;

    private SessionCommand()
    {
    }

    @Override
    protected ConsoleReader createConsoleReader( @Nonnull Environment env ) throws IOException
    {
        // create history
        Path etcDir = this.server.getEtcPath();
        Path historyDir = etcDir.resolve( "ssh" ).resolve( "history" );
        this.history = new FileHistory( historyDir.resolve( getSession().getUsername() ).toFile() );

        ConsoleReader consoleReader = super.createConsoleReader( env );
        consoleReader.setHistoryEnabled( true );
        consoleReader.setHistory( this.history );
        consoleReader.setPrompt( "[mosaic > " + getSession().getUsername() + "]$ " );
        consoleReader.addCompleter( new CommandNameCompleter() );
        return consoleReader;
    }

    @Override
    protected void onStart( @Nonnull Console console ) throws IOException
    {
        console.println()
               .println( "*************************************************************" )
               .println()
               .println( "Welcome to Mosaic! (running on " + System.getProperty( "os.name" ) + ")" )
               .println( "-------------------------------------------------------------" )
               .println()
               .println( "Usage" )
               .println( "-----" )
               .println()
               .println( "Type 'help' to view available commands." )
               .println( "Tab-completion is enabled (if your console supports it too)." )
               .println( "" )
               .println( "*************************************************************" )
               .println();
    }

    @Override
    protected void execute( @Nonnull Console console ) throws IOException
    {
        // input loop
        while( !Thread.currentThread().isInterrupted() )
        {
            String line;
            try
            {
                line = console.readLine();
            }
            catch( CommandCanceledException e )
            {
                continue;
            }

            // save history to file
            saveHistory();

            // if null returned from 'readLine' it means the input has EOF'ed, so we should exit
            if( line == null )
            {
                // session exited by user, or end of stream
                break;
            }
            else if( line.isEmpty() )
            {
                continue;
            }

            // run command
            try
            {
                processInputLine( console, line );
            }
            catch( QuitSessionException e )
            {
                break;
            }
            catch( CommandCanceledException ignore )
            {
            }
        }
    }

    @Override
    protected void onStop( @Nonnull Console console )
    {
        saveHistory();
    }

    private void saveHistory()
    {
        FileHistory history = this.history;
        if( history != null )
        {
            try
            {
                history.flush();
            }
            catch( IOException e )
            {
                LOG.warn( "Could not save shell command history: {}", e.getMessage(), e );
            }
        }
    }
}
