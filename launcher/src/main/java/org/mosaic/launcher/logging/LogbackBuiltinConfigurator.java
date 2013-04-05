package org.mosaic.launcher.logging;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class LogbackBuiltinConfigurator extends JoranConfigurator
{
    @Nonnull
    private final org.mosaic.launcher.logging.AppenderRegistry appenderRegistry;

    public LogbackBuiltinConfigurator( @Nonnull AppenderRegistry appenderRegistry )
    {
        this.appenderRegistry = appenderRegistry;
    }

    @Override
    public void addInstanceRules( @Nonnull final RuleStore rs )
    {
        rs.addRule( new Pattern( "configuration/appender" ), new RegisterAppenderAction( this.appenderRegistry ) );
        super.addInstanceRules( rs );
    }
}
