package org.mosaic.shell.impl.session;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jline.console.ConsoleReader;
import jline.console.history.History;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Server;
import org.mosaic.shell.impl.command.CommandManager;

/**
 * @author arik
 */
public class MosaicCommand extends AbstractSshdCommand
{
    @Nonnull
    private final CommandManager commandsManager;

    @Nonnull
    private final String line;

    public MosaicCommand( @Nonnull Server server, @Nonnull CommandManager commandsManager, @Nonnull String line )
    {
        super( server );
        this.commandsManager = commandsManager;
        this.line = line.trim();
    }

    @Override
    protected void startInternal( @Nonnull Environment env,
                                  @Nonnull InputStream in,
                                  @Nonnull OutputStream out,
                                  @Nonnull OutputStream err,
                                  @Nonnull ServerSession session,
                                  @Nonnull History history ) throws Exception
    {
        // no-op
    }

    @Override
    protected void runInternal( @Nonnull Environment env,
                                @Nonnull InputStream in,
                                @Nonnull OutputStream out,
                                @Nonnull OutputStream err,
                                @Nonnull ServerSession session,
                                @Nonnull History history ) throws Exception
    {
        if( this.line.length() > 0 )
        {
            ByteArrayInputStream linein = new ByteArrayInputStream( this.line.getBytes( "UTF-8" ) );
            ConsoleReader consoleReader = new ConsoleReader( linein, out, new SessionTerminal( env, 200, 200 ) );
            ConsoleImpl console = new ConsoleImpl( consoleReader );
            setExitCode( this.commandsManager.execute( console, line ) );
        }
    }

    @Override
    protected void destroyInternal() throws Exception
    {
        // no-op
    }
}
