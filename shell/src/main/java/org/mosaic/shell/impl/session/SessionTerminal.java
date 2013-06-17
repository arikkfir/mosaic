package org.mosaic.shell.impl.session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jline.TerminalSupport;
import org.apache.sshd.server.Environment;

/**
 * @author arik
 */
public class SessionTerminal extends TerminalSupport
{
    @Nonnull
    private final Environment environment;

    @Nullable
    private Integer customWidth;

    @Nullable
    private Integer customHeight;

    public SessionTerminal( @Nonnull Environment environment )
    {
        super( true );
        this.environment = environment;
    }

    public SessionTerminal( @Nonnull Environment environment,
                            @Nullable Integer customWidth,
                            @Nullable Integer customHeight )
    {
        super( true );
        this.environment = environment;
        this.customWidth = customWidth;
        this.customHeight = customHeight;
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
        if( this.customWidth != null )
        {
            return this.customWidth;
        }
        String value = this.environment.getEnv().get( Environment.ENV_COLUMNS );
        return value == null ? 80 : Integer.valueOf( value );
    }

    @Override
    public int getHeight()
    {
        if( this.customHeight != null )
        {
            return this.customHeight;
        }
        String value = this.environment.getEnv().get( Environment.ENV_LINES );
        return value == null ? 24 : Integer.valueOf( value );
    }
}
