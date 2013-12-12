package org.mosaic.console.remote.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.ConsoleReader;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.console.Console;
import org.mosaic.console.spi.CommandManager;
import org.mosaic.console.spi.QuitSessionException;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;

/**
 * @author arik
 */
abstract class AbstractSshdCommand implements Command, SessionAware
{
    @Service
    @Nonnull
    private CommandManager commandManager;

    @Nonnull
    private InputStream in;

    @Nonnull
    private OutputStream out;

    @Nonnull
    private ExitCallback exitCallback;

    @Nonnull
    private ServerSession session;

    @Nullable
    private RunLoop runLoop;

    @Nonnull
    @Service
    private Security security;

    protected AbstractSshdCommand()
    {
    }

    @Override
    public final void setInputStream( @Nonnull InputStream in )
    {
        this.in = in;
    }

    @Override
    public final void setOutputStream( @Nonnull OutputStream out )
    {
        this.out = new LfToCrLfFilterOutputStream( out );
    }

    @Override
    public final void setErrorStream( @Nonnull OutputStream err )
    {
        // no-op
    }

    @Override
    public final void setExitCallback( @Nonnull ExitCallback callback )
    {
        this.exitCallback = callback;
    }

    @Nonnull
    public final ServerSession getSession()
    {
        return session;
    }

    @Override
    public final void setSession( @Nonnull ServerSession session )
    {
        this.session = session;
    }

    @Override
    public final void start( @Nonnull Environment env ) throws IOException
    {
        try
        {
            this.runLoop = new RunLoop( env );
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Could not create shell session: " + e.getMessage(), e );
        }

        Thread thread = new Thread( this.runLoop, "MosaicShellConsole-" + this.session.getUsername() );
        thread.setDaemon( true );
        thread.start();
    }

    @Override
    public final void destroy()
    {
        if( this.runLoop != null )
        {
            Thread thread = this.runLoop.thread;
            if( thread != null )
            {
                thread.interrupt();
            }
        }
    }

    protected ConsoleReader createConsoleReader( @Nonnull Environment env ) throws IOException
    {
        ConsoleReader consoleReader = new ConsoleReader( "MosaicShell", in, out, new ConsoleTerminal( env ) );
        consoleReader.setBellEnabled( true );
        consoleReader.setHandleUserInterrupt( true );
        return consoleReader;
    }

    protected void processInputLine( @Nonnull Console console, @Nonnull String line ) throws IOException
    {
        try
        {
            this.commandManager.execute( console, line );
        }
        catch( QuitSessionException e )
        {
            throw e;
        }
        catch( Throwable e )
        {
            console.printStackTrace( e );
        }
    }

    protected void onStart( @Nonnull Console console ) throws IOException
    {
        // no-op
    }

    protected abstract void execute( @Nonnull Console console ) throws IOException;

    protected void onStop( @Nonnull Console console )
    {
        // no-op
    }

    private class RunLoop implements Runnable
    {
        @Nonnull
        private final ConsoleReader consoleReader;

        @Nonnull
        private final Console console;

        @Nullable
        private Thread thread;

        private RunLoop( @Nonnull Environment env ) throws IOException
        {
            this.consoleReader = createConsoleReader( env );
            this.console = new ConsoleImpl( this.consoleReader );
        }

        @Override
        public void run()
        {
            this.thread = Thread.currentThread();

            Subject subject = getSession().getAttribute( SshServer.SUBJECT_KEY );
            if( subject != null )
            {
                subject.login();
                try
                {
                    onStart( this.console );
                    execute( this.console );
                }
                catch( Exception e )
                {
                    try
                    {
                        this.console.printStackTrace( e );
                    }
                    catch( Exception ignore )
                    {
                    }
                }
                finally
                {
                    try
                    {
                        onStop( this.console );
                    }
                    catch( Throwable e )
                    {
                        try
                        {
                            this.console.printStackTrace( e );
                        }
                        catch( Exception ignore )
                        {
                        }
                    }
                    subject.logout();
                }
            }
            else
            {
                try
                {
                    this.console.println( "Could not initialize security session subject." );
                }
                catch( Throwable ignore )
                {
                }
            }

            // exit code
            exit();
        }

        private void exit()
        {
            try
            {
                this.consoleReader.flush();
            }
            catch( IOException ignore )
            {
            }
            exitCallback.onExit( 0 );
        }
    }
}
