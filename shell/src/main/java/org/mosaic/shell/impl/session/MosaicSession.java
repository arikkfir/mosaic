package org.mosaic.shell.impl.session;

import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.console.history.History;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.Server;
import org.mosaic.shell.impl.command.CommandManager;

/**
 * @author arik
 */
public class MosaicSession extends AbstractSshdCommand
{
    @Nonnull
    private final CommandManager commandsManager;

    @Nonnull
    private final MosaicCommandCompleter commandCompleter;

    @Nullable
    private ConsoleImpl console;

    public MosaicSession( @Nonnull Server server,
                          @Nonnull MosaicCommandCompleter commandCompleter,
                          @Nonnull CommandManager commandsManager )
    {
        super( server );
        this.commandCompleter = commandCompleter;
        this.commandsManager = commandsManager;
    }

    @Override
    protected void startInternal( @Nonnull Environment env,
                                  @Nonnull InputStream in,
                                  @Nonnull OutputStream out,
                                  @Nonnull OutputStream err,
                                  @Nonnull ServerSession session,
                                  @Nonnull History history ) throws Exception
    {
        this.console = new ConsoleImpl( new MosaicConsoleReader( in,
                                                                 out,
                                                                 env,
                                                                 history,
                                                                 getUsername(),
                                                                 this.commandCompleter ) );
    }

    @Override
    protected void runInternal( @Nonnull Environment env,
                                @Nonnull InputStream in,
                                @Nonnull OutputStream out,
                                @Nonnull OutputStream err,
                                @Nonnull ServerSession session,
                                @Nonnull History history ) throws Exception
    {
        ConsoleImpl console = this.console;
        if( console != null )
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

            while( true )
            {
                String line = console.readLine().trim();
                if( line == null )
                {
                    break;
                }

                line = line.trim();
                if( !line.isEmpty() )
                {
                    try
                    {
                        this.commandsManager.execute( console, line );
                    }
                    catch( Exception e )
                    {
                        setExitCode( -1 );
                        setExitMessage( "Internal error: " + e.getMessage() );
                        console.printStackTrace( e );
                    }
                }
            }
        }
    }

    @Override
    protected void destroyInternal() throws Exception
    {
        this.console = null;
    }
}
