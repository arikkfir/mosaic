package org.mosaic.server.shell.impl;

import jline.TerminalSupport;
import org.apache.sshd.server.Environment;

/**
 * @author arik
 */
public class ShellTerminal extends TerminalSupport {

    private final Environment environment;

    public ShellTerminal( Environment environment ) {
        super( true );
        this.environment = environment;
    }

    @Override
    public void init() throws Exception {
        super.init();
        setAnsiSupported( true );
        setEchoEnabled( false );
    }

    @Override
    public int getWidth() {
        String value = this.environment.getEnv().get( Environment.ENV_COLUMNS );
        return value == null ? 80 : Integer.valueOf( value );
    }

    @Override
    public int getHeight() {
        String value = this.environment.getEnv().get( Environment.ENV_LINES );
        return value == null ? 24 : Integer.valueOf( value );
    }
}
