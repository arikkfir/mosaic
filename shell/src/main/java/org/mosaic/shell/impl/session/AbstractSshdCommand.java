package org.mosaic.shell.impl.session;

import java.io.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.history.FileHistory;
import jline.console.history.History;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public abstract class AbstractSshdCommand implements Command, SessionAware, Runnable
{
    private final Logger LOG = LoggerFactory.getLogger( getClass() );

    @Nonnull
    private Server server;

    @Nullable
    private InputStream in;

    @Nullable
    private OutputStream out;

    @Nullable
    private OutputStream err;

    @Nullable
    private ExitCallback exitCallback;

    @Nullable
    private ServerSession session;

    @Nonnull
    private jline.console.history.History history = new jline.console.history.MemoryHistory();

    @Nullable
    private Environment env;

    private int exitCode;

    @Nullable
    private String exitMessage;

    public AbstractSshdCommand( @Nonnull Server server )
    {
        this.server = server;
    }

    @Override
    public void setInputStream( @Nonnull InputStream in )
    {
        this.in = in;
    }

    @Override
    public void setOutputStream( @Nonnull OutputStream out )
    {
        this.out = new LfToCrLfFilterOutputStream( out );
    }

    @Override
    public void setErrorStream( @Nonnull OutputStream err )
    {
        this.err = new LfToCrLfFilterOutputStream( err );
    }

    @Override
    public void setExitCallback( @Nullable ExitCallback callback )
    {
        this.exitCallback = callback;
    }

    @Override
    public void setSession( @Nullable ServerSession session )
    {
        this.session = session;
    }

    public void setExitCode( int exitCode )
    {
        this.exitCode = exitCode;
    }

    public void setExitMessage( @Nullable String exitMessage )
    {
        this.exitMessage = exitMessage;
    }

    @Override
    public final void start( @Nonnull Environment env ) throws IOException
    {
        this.env = env;
        this.history = new FileHistory( this.server.getWork().resolve( "shell-history" ).resolve( getUsername() ).toFile() );

        try
        {
            if( this.in != null && this.out != null && this.err != null && this.session != null )
            {
                startInternal( this.env, this.in, this.out, this.err, this.session, this.history );
            }
        }
        catch( IOException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "Could not start " + getClass().getSimpleName() + ": " + e.getMessage(), e );
        }

        Thread thread = new Thread( this, "MosaicShellConsole" );
        thread.setDaemon( true );
        thread.start();
    }

    @Override
    public final void run()
    {
        try
        {
            if( this.env != null && this.in != null && this.out != null && this.err != null && this.session != null )
            {
                runInternal( this.env, this.in, this.out, this.err, this.session, this.history );
            }
        }
        catch( Exception e )
        {
            if( this.err != null )
            {
                e.printStackTrace( new PrintWriter( this.err ) );
            }
            else
            {
                LOG.error( "Error in Mosaic shell session: {}", e.getMessage(), e );
            }
            setExitCode( -1 );
            setExitMessage( "Internal error: " + e.getMessage() );
        }
        finally
        {
            if( this.exitCallback != null )
            {
                this.exitCallback.onExit( this.exitCode, this.exitMessage == null ? "" : this.exitMessage );
            }
        }
    }

    @Override
    public final void destroy()
    {
        if( this.history instanceof Flushable )
        {
            Flushable flushable = ( Flushable ) this.history;
            try
            {
                flushable.flush();
            }
            catch( IOException e )
            {
                LOG.warn( "Could not save shell command history: {}", e.getMessage(), e );
            }
        }

        this.session = null;

        try
        {
            destroyInternal();
        }
        catch( Exception e )
        {
            LOG.error( "Ignoring error that occurred while destorying {}: {}", getClass().getSimpleName(), e.getMessage(), e );
        }

        try
        {
            if( this.out != null )
            {
                this.out.flush();
                this.out.close();
                this.out = null;
            }
        }
        catch( IOException ignore )
        {
        }

        try
        {
            if( this.err != null )
            {
                this.err.flush();
                this.err.close();
                this.err = null;
            }
        }
        catch( IOException ignore )
        {
        }

        this.env = null;
    }

    protected final String getUsername()
    {
        return this.session == null ? "anonymous" : this.session.getUsername();
    }

    protected abstract void startInternal( @Nonnull Environment env,
                                           @Nonnull InputStream in,
                                           @Nonnull OutputStream out,
                                           @Nonnull OutputStream err,
                                           @Nonnull ServerSession session,
                                           @Nonnull History history ) throws Exception;

    protected abstract void runInternal( @Nonnull Environment env,
                                         @Nonnull InputStream in,
                                         @Nonnull OutputStream out,
                                         @Nonnull OutputStream err,
                                         @Nonnull ServerSession session,
                                         @Nonnull History history ) throws Exception;

    protected abstract void destroyInternal() throws Exception;
}
