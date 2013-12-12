package org.mosaic.console.remote.impl;

import javax.annotation.Nonnull;
import jline.TerminalSupport;
import org.apache.sshd.server.Environment;

/**
 * @author arik
 */
final class ConsoleTerminal extends TerminalSupport
{
    @Nonnull
    private final Environment environment;

    public ConsoleTerminal( @Nonnull Environment env )
    {
        super( true );
        this.environment = env;
    }

    @Override
    public void init() throws Exception
    {
        super.init();
        setAnsiSupported( true );
        setEchoEnabled( false );
    }

    @Override
    public int getWidth()
    {
        return Integer.parseInt( this.environment.getEnv().get( "COLUMNS" ) );
    }

    @Override
    public int getHeight()
    {
        return Integer.parseInt( this.environment.getEnv().get( "LINES" ) );
    }
}
