package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jline.console.ConsoleReader;
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
import org.mosaic.server.shell.impl.command.CommandInfo;
import org.mosaic.server.shell.impl.command.ShellCommandsManager;
import org.mosaic.server.shell.impl.io.IoUtils;
import org.mosaic.server.shell.impl.io.LfToCrLfFilterOutputStream;
import org.mosaic.server.shell.impl.io.Pipe;
import org.mosaic.server.shell.impl.io.PipeInputStream;
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
            this.consoleReader.setHistoryEnabled( true );
            this.consoleReader.setPrompt( "[mosaic@host.com]$ " );
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
        long start = System.currentTimeMillis();
        ShellConsole shellConsole = new ShellConsole( this.consoleReader );
        try {
            WelcomeMessage.print( this.bundleContext, shellConsole );
            String line = shellConsole.readLine();
            while( line != null ) {
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

                    CommandInfo command = this.commandsManager.getCommand( tokens[ 0 ] );
                    if( command == null ) {
                        shellConsole.println( "Unknown command: " + tokens[ 0 ] );
                    } else {
                        try {
                            command.execute( shellConsole, args );
                        } catch( OptionException e ) {
                            shellConsole.println( e.getMessage() );
                        } catch( Exception e ) {
                            PrintWriter printWriter = new PrintWriter( this.consoleReader.getOutput() );
                            e.printStackTrace( printWriter );
                            printWriter.flush();
                        }
                    }
                }
                line = shellConsole.readLine();
            }

            String duration = SESSION_DURATION_FORMATTER.print( new Period( start, System.currentTimeMillis() ) );
            shellConsole.println();
            shellConsole.println( "Goodbye! (session was " + duration + " long)" );
            shellConsole.flush();

        } catch( IOException e ) {
            LOG.error( "I/O error occurred in SSH session: {}", e.getMessage(), e );
            this.session.close( true );
        }
    }

    @Override
    public void destroy() {
        this.consoleThread.interrupt();
        try {
            this.consoleThread.join();
        } catch( InterruptedException ignore ) {
        }

        this.inputThread.interrupt();
        try {
            this.inputThread.join();
        } catch( InterruptedException ignore ) {
        }

        IoUtils.flush( out, err );
        IoUtils.close( in, out, err );

        if( this.exitCallback != null ) {
            this.exitCallback.onExit( 0 );
        }

        this.inputQueue = null;
    }
}
