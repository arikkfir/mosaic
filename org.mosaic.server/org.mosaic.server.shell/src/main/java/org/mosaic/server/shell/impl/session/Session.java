package org.mosaic.server.shell.impl.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import joptsimple.OptionException;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Home;
import org.mosaic.lifecycle.ContextRef;
import org.mosaic.server.shell.ExitSessionException;
import org.mosaic.server.shell.impl.command.ShellCommand;
import org.mosaic.server.shell.impl.command.ShellCommandsManager;
import org.mosaic.server.shell.impl.util.IoUtils;
import org.mosaic.server.shell.impl.util.LfToCrLfFilterOutputStream;
import org.mosaic.server.shell.impl.util.Pipe;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@Scope( "prototype" )
public class Session implements Command, Runnable, SessionAware
{

    private static final Logger LOG = LoggerFactory.getLogger( Session.class );

    private BundleContext bundleContext;

    private Home home;

    private ShellCommandsManager commandsManager;

    private BlockingQueue<Integer> inputQueue;

    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    private ExitCallback exitCallback;

    private ConsoleReader consoleReader;

    private Thread consoleThread;

    private Thread inputThread;

    private MosaicServerSession session;

    private boolean running;

    private FileHistory history;

    @Autowired
    public void setCommandsManager( ShellCommandsManager commandsManager )
    {
        this.commandsManager = commandsManager;
    }

    /**
     * Sets the Mosaic home. Used for storing the shell history files for users.
     * <p/>
     * NOT annotated with @ServiceRef because this bean is in the "prototype" scope which is not supported for service
     * injection.
     *
     * @param home the mosaic home directory
     */
    public void setHome( Home home )
    {
        this.home = home;
    }

    @ContextRef
    public void setBundleContext( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
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
        this.inputQueue = new LinkedBlockingQueue<>( 100000 );

        // the thread which populates the input queue from the ssh session's input stream
        this.inputThread = new Thread( new Pipe( this.in, this.inputQueue ), "SSHD/input" );
        this.inputThread.setDaemon( true );
        this.inputThread.start( );

        // create the console reader used for interacting with the user
        try
        {
            String username = this.session.getUsername( );
            Path historyFile = this.home.getWork( ).resolve( "history/" + username );
            this.history = new FileHistory( historyFile.toFile( ) );

            this.consoleReader =
                    new ConsoleReader( new PipeInputStream( this.inputQueue ), this.out, new SshTerminal( env ) );
            this.consoleReader.setHistory( history );
            this.consoleReader.setPrompt( "[" + this.session.getUsername( ) + "@host.com]$ " );
            this.consoleReader.addCompleter( new CommandCompleter( ) );

        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Cannot start SSH console session: " + e.getMessage( ), e );
        }

        // start the console thread, using 'this' as the runnable
        this.consoleThread = new Thread( this, "SSHD/console" );
        this.consoleThread.setDaemon( true );
        this.consoleThread.start( );
    }

    @Override
    public void run( )
    {
        this.running = true;

        ConsoleImpl console = new ConsoleImpl( this.consoleReader );
        try
        {
            WelcomeMessage.print( this.bundleContext, console );
            String line;
            while( this.running && ( line = console.readLine( ) ) != null )
            {
                line = line.trim( );
                if( line.length( ) > 0 )
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

                    ShellCommand shellCommand = this.commandsManager.getCommand( tokens[ 0 ] );
                    if( shellCommand == null )
                    {
                        console.println( "Unknown command: " + tokens[ 0 ] );
                    }
                    else
                    {
                        try
                        {
                            shellCommand.execute( console, args );
                        }
                        catch( ExitSessionException e )
                        {
                            this.inputQueue.offer( -1 );
                        }
                        catch( OptionException e )
                        {
                            if( this.running )
                            {
                                console.println( e.getMessage( ) );
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
                this.history.flush( );
            }
            catch( IOException ignore )
            {
            }

        }
        catch( IOException e )
        {
            LOG.error( "I/O error occurred in SSH session: {}", e.getMessage( ), e );
        }
        finally
        {
            this.running = false;
            if( !this.session.isClosing( ) )
            {
                try
                {
                    console.println( );
                    console.println( "Goodbye!" );
                    console.flush( );
                    this.session.close( false );
                }
                catch( IOException ignore )
                {
                }
            }
        }

    }

    @Override
    public void destroy( )
    {

        this.running = false;
        this.consoleThread.interrupt( );
        this.inputThread.interrupt( );

        IoUtils.flush( out, err );
        IoUtils.close( in, out, err );

        if( this.exitCallback != null )
        {
            this.exitCallback.onExit( 0 );
        }

        this.inputQueue = null;
    }

    private class CommandCompleter implements Completer
    {

        @Override
        public int complete( String buffer, int cursor, List<CharSequence> candidates )
        {
            if( candidates == null )
            {
                return -1;
            }

            Collection<ShellCommand> commands = commandsManager.getCommands( );
            SortedSet<String> commandNames = new TreeSet<>( );
            for( ShellCommand command : commands )
            {
                commandNames.add( command.getName( ) );
            }

            if( buffer == null )
            {
                candidates.addAll( commandNames );
            }
            else
            {
                for( String match : commandNames.tailSet( buffer ) )
                {
                    if( !match.startsWith( buffer ) )
                    {
                        break;
                    }
                    candidates.add( match );
                }
            }
            if( candidates.size( ) == 1 )
            {
                candidates.set( 0, candidates.get( 0 ) + " " );
            }
            return candidates.isEmpty( ) ? -1 : 0;
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
        public int read( ) throws IOException
        {
            try
            {
                if( running )
                {
                    return inputQueue.take( );
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
