package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import joptsimple.OptionException;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.mosaic.lifecycle.BundleContextAware;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.shell.impl.command.ShellCommand;
import org.mosaic.server.shell.impl.command.ShellCommandsManager;
import org.mosaic.server.shell.impl.io.IoUtils;
import org.mosaic.server.shell.impl.io.LfToCrLfFilterOutputStream;
import org.mosaic.server.shell.impl.io.Pipe;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@Scope( "prototype" )
public class Shell implements Command, Runnable, SessionAware, BundleContextAware {

    private static final Logger LOG = LoggerFactory.getBundleLogger( Shell.class );

    private static final PeriodFormatter SESSION_DURATION_FORMATTER = new PeriodFormatterBuilder()
            .appendDays()
            .appendSuffix( " day", " days" )
            .appendSeparator( ", " )
            .appendHours()
            .appendSuffix( " hour", " hours" )
            .appendSeparator( ", " )
            .appendMinutes()
            .appendSuffix( " minute", " minutes" )
            .appendSeparator( " and " )
            .appendSeconds()
            .appendSuffix( " second", " seconds" )
            .toFormatter();

    private BundleContext bundleContext;

    private ShellCommandsManager commandsManager;

    private BlockingQueue<Integer> inputQueue;

    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    private ExitCallback exitCallback;

    private ConsoleReader consoleReader;

    private Thread consoleThread;

    private Thread inputThread;

    private ServerSession session;

    private boolean running;

    @Autowired
    public void setCommandsManager( ShellCommandsManager commandsManager ) {
        this.commandsManager = commandsManager;
    }

    @Override
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void setSession( ServerSession session ) {
        this.session = session;
    }

    @Override
    public void setInputStream( InputStream in ) {
        this.in = in;
    }

    @Override
    public void setOutputStream( OutputStream out ) {
        this.out = new LfToCrLfFilterOutputStream( out );
    }

    @Override
    public void setErrorStream( OutputStream err ) {
        this.err = new LfToCrLfFilterOutputStream( err );
    }

    @Override
    public void setExitCallback( ExitCallback callback ) {
        this.exitCallback = callback;
    }

    @Override
    public void start( Environment env ) throws IOException {
        // a queue populated by a thread which reads from the session's "in" into this queue
        // this way, if a command processing takes a long time, the session's input buffer won't be overloaded
        // because we will buffer it ourselves in this queue
        this.inputQueue = new LinkedBlockingQueue<>( 100000 );

        // the thread which populates the input queue from the ssh session's input stream
        this.inputThread = new Thread( new Pipe( this.in, this.inputQueue ), "SSHD/input" );
        this.inputThread.setDaemon( true );
        this.inputThread.start();

        // create the console reader used for interacting with the user
        try {
            this.consoleReader = new ConsoleReader( new PipeInputStream( this.inputQueue ), this.out, new ShellTerminal( env ) );
            //TODO: this.consoleReader.setHistoryEnabled( true );
            this.consoleReader.setPrompt( "[mosaic@host.com]$ " );
            this.consoleReader.addCompleter( new CommandCompleter() );

        } catch( Exception e ) {
            throw new IllegalStateException( "Cannot start SSH console session: " + e.getMessage(), e );
        }

        // start the console thread, using 'this' as the runnable
        this.consoleThread = new Thread( this, "SSHD/console" );
        this.consoleThread.setDaemon( true );
        this.consoleThread.start();
    }

    @Override
    public void run() {
        this.running = true;

        long start = System.currentTimeMillis();
        ShellConsole shellConsole = new ShellConsole( this.consoleReader );
        try {
            WelcomeMessage.print( this.bundleContext, shellConsole );
            String line;
            while( this.running && ( line = shellConsole.readLine() ) != null ) {
                line = line.trim();
                if( line.length() > 0 ) {
                    String[] tokens = line.split( " " );

                    String[] args;
                    if( tokens.length == 1 ) {
                        args = new String[ 0 ];
                    } else {
                        args = new String[ tokens.length - 1 ];
                        System.arraycopy( tokens, 1, args, 0, args.length );
                    }

                    ShellCommand shellCommand = this.commandsManager.getCommand( tokens[ 0 ] );
                    if( shellCommand == null ) {
                        shellConsole.println( "Unknown command: " + tokens[ 0 ] );
                    } else {
                        try {
                            shellCommand.execute( shellConsole, args );
                        } catch( OptionException e ) {
                            if( this.running ) {
                                shellConsole.println( e.getMessage() );
                            }
                        } catch( Exception e ) {
                            if( this.running ) {
                                shellConsole.printStackTrace( e );
                            }
                        }
                    }
                }
            }

            if( this.running ) {
                String duration = SESSION_DURATION_FORMATTER.print( new Period( start, System.currentTimeMillis() ) );
                shellConsole.println();
                shellConsole.println( "Goodbye! (session was " + duration + " long)" );
                shellConsole.flush();
            }
            this.session.disconnect( 0, "" );

        } catch( IOException e ) {
            LOG.error( "I/O error occurred in SSH session: {}", e.getMessage(), e );
            this.session.close( true );
        } finally {
            this.running = false;
        }
    }

    @Override
    public void destroy() {
        this.running = false;
        this.consoleThread.interrupt();
        this.inputThread.interrupt();

        IoUtils.flush( out, err );
        IoUtils.close( in, out, err );

        if( this.exitCallback != null ) {
            this.exitCallback.onExit( 0 );
        }

        this.inputQueue = null;
    }

    private class CommandCompleter implements Completer {

        @Override
        public int complete( String buffer, int cursor, List<CharSequence> candidates ) {
            if( candidates == null ) {
                return -1;
            }

            Collection<ShellCommand> commands = commandsManager.getCommands();
            SortedSet<String> commandNames = new TreeSet<>();
            for( ShellCommand command : commands ) {
                commandNames.add( command.getName() );
            }

            if( buffer == null ) {
                candidates.addAll( commandNames );
            } else {
                for( String match : commandNames.tailSet( buffer ) ) {
                    if( !match.startsWith( buffer ) ) {
                        break;
                    }
                    candidates.add( match );
                }
            }
            if( candidates.size() == 1 ) {
                candidates.set( 0, candidates.get( 0 ) + " " );
            }
            return candidates.isEmpty() ? -1 : 0;
        }
    }

    private class PipeInputStream extends InputStream {

        private final BlockingQueue<Integer> inputQueue;

        private PipeInputStream( BlockingQueue<Integer> inputQueue ) {
            this.inputQueue = inputQueue;
        }

        @Override
        public int read() throws IOException {
            try {
/*
            Integer poll = inputQueue.poll( 3, TimeUnit.SECONDS );
            while( poll == null ) {
                poll = inputQueue.poll( 3, TimeUnit.SECONDS );
            }
*/
                if( running ) {
                    return inputQueue.take();
                } else {
                    return -1;
                }

            } catch( InterruptedException e ) {
                return -1;
            }
        }
    }
}
