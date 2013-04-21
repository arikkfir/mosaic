package org.mosaic.shell.impl.session;

import com.google.common.io.Closeables;
import com.google.common.io.Flushables;
import java.io.ByteArrayInputStream;
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
import org.mosaic.shell.CommandExecutionException;
import org.mosaic.shell.TerminateSessionException;
import org.mosaic.shell.impl.command.CommandManager;

/**
 * @author arik
 */
public class MosaicCommand implements Command, SessionAware
{
    @Nonnull
    private final CommandManager commandsManager;

    @Nonnull
    private final String line;

    @Nullable
    private InputStream in;

    @Nullable
    private OutputStream out;

    @Nullable
    private OutputStream err;

    @Nullable
    private ExitCallback exitCallback;

    @Nullable
    private MosaicServerSession session;

    public MosaicCommand( @Nonnull CommandManager commandsManager, @Nonnull String line )
    {
        this.commandsManager = commandsManager;
        this.line = line.trim();
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
    public void setExitCallback( @Nullable ExitCallback callback )
    {
        this.exitCallback = callback;
    }

    @Override
    public void setSession( @Nullable ServerSession session )
    {
        this.session = ( MosaicServerSession ) session;
    }

    @Override
    public void start( Environment env ) throws IOException
    {
        int exitCode = 0;

        if( this.line.length() > 0 )
        {
            ByteArrayInputStream in = new ByteArrayInputStream( this.line.getBytes( "UTF-8" ) );
            ConsoleReader consoleReader = new ConsoleReader( in, this.out, new SessionTerminal( env, 200, 200 ) );
            ConsoleImpl console = new ConsoleImpl( consoleReader );
            try
            {
                exitCode = this.commandsManager.execute( console, line );
            }
            catch( TerminateSessionException ignore )
            {
                // we can ignore this since the command already finished, we can gracefuly exit
            }
            catch( CommandExecutionException e )
            {
                console.printStackTrace( e );
                exitCode = e.getExitCode();
            }
            catch( Exception e )
            {
                console.printStackTrace( e );
                exitCode = 1;
            }
        }

        if( this.exitCallback != null )
        {
            this.exitCallback.onExit( exitCode );
        }
        if( this.session != null )
        {
            this.session.close( false );
        }
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void destroy()
    {
        Flushables.flushQuietly( out );
        Flushables.flushQuietly( err );
        Closeables.closeQuietly( in );
        Closeables.closeQuietly( out );
        Closeables.closeQuietly( err );
    }
}
