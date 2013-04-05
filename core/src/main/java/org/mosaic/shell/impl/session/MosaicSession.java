package org.mosaic.shell.impl.session;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.shell.Console;
import org.mosaic.shell.TerminateSessionException;
import org.mosaic.shell.impl.command.CommandExecutor;
import org.mosaic.shell.impl.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class MosaicSession implements Command, Runnable, SessionAware
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicSession.class );

    private static class LfToCrLfFilterOutputStream extends FilterOutputStream
    {
        private boolean lastWasCr;

        public LfToCrLfFilterOutputStream( OutputStream out )
        {
            super( out );
        }

        @Override
        public void write( int b ) throws IOException
        {
            if( !lastWasCr && b == '\n' )
            {
                out.write( '\r' );
                out.write( '\n' );
            }
            else
            {
                out.write( b );
            }
            lastWasCr = b == '\r';
        }
    }

    @Nonnull
    private final ModuleManager moduleManager;

    @Nonnull
    private final CommandManager commandsManager;

    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    private ExitCallback exitCallback;

    private ConsoleReader consoleReader;

    private Thread consoleThread;

    private InputQueueThread inputQueueThread;

    private MosaicServerSession session;

    private boolean running;

    private FileHistory history;

    public MosaicSession( @Nonnull ModuleManager moduleManager, @Nonnull CommandManager commandsManager )
    {
        this.moduleManager = moduleManager;
        this.commandsManager = commandsManager;
    }

    @Override
    public void setSession( ServerSession session )
    {
        this.session = ( MosaicServerSession ) session;
    }

    @Override
    public void setInputStream( InputStream in )
    {
        this.in = in;
    }

    @Override
    public void setOutputStream( OutputStream out )
    {
        this.out = new LfToCrLfFilterOutputStream( out );
    }

    @Override
    public void setErrorStream( OutputStream err )
    {
        this.err = new LfToCrLfFilterOutputStream( err );
    }

    @Override
    public void setExitCallback( ExitCallback callback )
    {
        this.exitCallback = callback;
    }

    @Override
    public void start( Environment env ) throws IOException
    {
        // a queue populated by a thread which reads from the session's "in" into this queue
        // this way, if a command processing takes a long time, the session's input buffer won't be overloaded
        // because we will buffer it ourselves in this queue
        this.inputQueueThread = new InputQueueThread( this.in );
        this.inputQueueThread.setDaemon( true );
        this.inputQueueThread.start();

        // create the console reader used for interacting with the user
        try
        {
            String username = this.session == null ? "anonymous" : this.session.getUsername();
            Path historyFile = Paths.get( System.getProperty( "mosaic.home.work" ), "history", username );
            this.history = new FileHistory( historyFile.toFile() );

            this.consoleReader = new ConsoleReader( new PipeInputStream( this.inputQueueThread.getBufferedInputQueue() ), this.out, new SessionTerminal( env ) );
            this.consoleReader.setHistory( history );
            this.consoleReader.setPrompt( "[" + username + "@" + InetAddress.getLocalHost().getHostName() + "]$ " );
            this.consoleReader.addCompleter( new CommandCompleter() );

        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Cannot start SSH console session: " + e.getMessage(), e );
        }

        // start the console thread, using 'this' as the runnable
        this.consoleThread = new Thread( this, "SSHD/console" );
        this.consoleThread.setDaemon( true );
        this.consoleThread.start();
    }

    @Override
    public void run()
    {
        this.running = true;

        ConsoleImpl console = new ConsoleImpl( this.consoleReader );
        try
        {
            printWelcomeMessage( console );
            String line;
            while( this.running && ( line = console.readLine() ) != null )
            {
                line = line.trim();
                if( line.length() > 0 )
                {
                    String[] tokens = line.split( " " );

                    String[] args;
                    if( tokens.length == 1 )
                    {
                        args = new String[ 0 ];
                    }
                    else
                    {
                        args = new String[ tokens.length - 1 ];
                        System.arraycopy( tokens, 1, args, 0, args.length );
                    }

                    CommandExecutor adapter = this.commandsManager.getCommand( tokens[ 0 ] );
                    if( adapter == null )
                    {
                        console.println( "Unknown command: " + tokens[ 0 ] );
                    }
                    else
                    {
                        try
                        {
                            adapter.execute( console, args );
                        }
                        catch( TerminateSessionException e )
                        {
                            if( this.session != null )
                            {
                                this.inputQueueThread.end();
                            }
                        }
                        catch( Exception e )
                        {
                            if( this.running )
                            {
                                console.printStackTrace( e );
                            }
                        }
                    }
                }
            }

            // persist history
            try
            {
                this.history.flush();
            }
            catch( IOException ignore )
            {
            }

        }
        catch( IOException e )
        {
            LOG.error( "I/O error occurred in SSH session: {}", e.getMessage(), e );
        }
        finally
        {
            this.running = false;
            if( this.session != null && !this.session.isClosing() )
            {
                try
                {
                    console.println();
                    console.println( "Goodbye!" );
                    console.flush();
                    this.session.close( false );
                }
                catch( IOException ignore )
                {
                }
            }
        }

    }

    @Override
    public void destroy()
    {
        this.running = false;
        this.consoleThread.interrupt();
        this.inputQueueThread.interrupt();

        for( Flushable flushable : asList( out, err ) )
        {
            try
            {
                flushable.flush();
            }
            catch( IOException ignore )
            {
            }
        }
        for( Closeable closeable : asList( in, out, err ) )
        {
            try
            {
                closeable.close();
            }
            catch( IOException ignore )
            {
            }
        }

        if( this.exitCallback != null )
        {
            this.exitCallback.onExit( 0 );
        }

        this.inputQueueThread.end();
        this.inputQueueThread = null;
    }

    private void printWelcomeMessage( Console console ) throws IOException
    {
        console.println()
               .println( "*************************************************************" )
               .println()
               .println( "Welcome to Rinku Server! (running on " + System.getProperty( "os.name" ) + ")" )
               .println( "-------------------------------------------------------------" )
               .println()
               .println( "Usage" )
               .println( "-----" )
               .println()
               .println( "Type 'help' to view available commands." )
               .println( "Tab-completion is enabled (unless you are running in IntelliJ, which disables TAB keys in consoles)." )
               .println( "" )
               .println( "*************************************************************" )
               .println();
    }

    private class CommandCompleter implements Completer
    {
        @Override
        public int complete( String buffer, int cursor, List<CharSequence> candidates )
        {
            int completionAnchorIndex = -1;

            // text up to the cursor
            String text = buffer == null || cursor < 0 ? "" : buffer.substring( 0, cursor );
            int firstSpace = text.indexOf( ' ' );
            if( firstSpace < 0 )
            {
                // find all commands starting with this text
                for( CommandExecutor adapter : commandsManager.getCommandExecutorsStartingWithPrefix( text ) )
                {
                    candidates.add( adapter.getCommand().getName() + " " );
                }
                completionAnchorIndex = 0;
            }
            else
            {
                CommandExecutor command = commandsManager.getCommand( text.substring( 0, firstSpace ) );
                if( command != null )
                {
                    String args = text.substring( firstSpace + 1 );
                    int argsCursor = cursor - firstSpace - 1;

                    int currentWordStart = 0;
                    int index = args.indexOf( ' ' );
                    while( index >= 0 && index < argsCursor )
                    {
                        currentWordStart = index;
                        index = args.indexOf( ' ', index + 1 );
                    }

                    int currentWordEnd = argsCursor - currentWordStart;
                    final String bundlePrefix = args.substring( currentWordStart, currentWordEnd );
                    candidates.addAll(
                            transform(
                                    filter(
                                            moduleManager.getModules(),
                                            new Predicate<Module>()
                                            {
                                                @Override
                                                public boolean apply( @Nullable Module input )
                                                {
                                                    return input != null && input.getName().startsWith( bundlePrefix );
                                                }
                                            }
                                    ),
                                    new Function<Module, String>()
                                    {
                                        @Nullable
                                        @Override
                                        public String apply( @Nullable Module input )
                                        {
                                            if( input != null )
                                            {
                                                return input.getName();
                                            }
                                            else
                                            {
                                                return "";
                                            }
                                        }
                                    }
                            )
                    );
                    completionAnchorIndex = firstSpace + 1;
                }
            }

            // return
            return completionAnchorIndex;
        }
    }

    private class PipeInputStream extends InputStream
    {
        private final BlockingQueue<Integer> inputQueue;

        private PipeInputStream( BlockingQueue<Integer> inputQueue )
        {
            this.inputQueue = inputQueue;
        }

        @Override
        public int read() throws IOException
        {
            try
            {
                if( running )
                {
                    return inputQueue.take();
                }
                else
                {
                    return -1;
                }

            }
            catch( InterruptedException e )
            {
                return -1;
            }
        }
    }
}