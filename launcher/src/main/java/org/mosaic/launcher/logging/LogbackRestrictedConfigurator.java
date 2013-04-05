package org.mosaic.launcher.logging;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class LogbackRestrictedConfigurator extends JoranConfigurator
{
    private static final List<Pattern> EXCLUDED_PATTERNS = Arrays.asList(
            new Pattern( "configuration/insertFromJNDI" ),
            new Pattern( "configuration/root/appender-ref" )
    );

    private static class RestrictedRuleStore implements RuleStore
    {
        @Nonnull
        private final RuleStore rs;

        public RestrictedRuleStore( @Nonnull RuleStore rs )
        {
            this.rs = rs;
        }

        @Override
        public void addRule( @Nonnull Pattern pattern, @Nonnull String actionClassStr ) throws ClassNotFoundException
        {
            for( Pattern excludedPattern : EXCLUDED_PATTERNS )
            {
                if( pattern.isContained( excludedPattern ) )
                {
                    return;
                }
            }
            rs.addRule( pattern, actionClassStr );
        }

        @Override
        public void addRule( @Nonnull Pattern pattern, @Nonnull Action action )
        {
            for( Pattern excludedPattern : EXCLUDED_PATTERNS )
            {
                if( pattern.isContained( excludedPattern ) )
                {
                    return;
                }
            }
            rs.addRule( pattern, action );
        }

        @Override
        public List matchActions( @Nonnull Pattern currentPattern )
        {
            return rs.matchActions( currentPattern );
        }
    }

    @Nonnull
    private final org.mosaic.launcher.logging.AppenderRegistry appenderRegistry;

    public LogbackRestrictedConfigurator( @Nonnull AppenderRegistry appenderRegistry )
    {
        this.appenderRegistry = appenderRegistry;
    }

    @Override
    public void addInstanceRules( @Nonnull final RuleStore rs )
    {
        rs.addRule( new Pattern( "*/appender" ), new RegisterAppenderAction( this.appenderRegistry ) );
        super.addInstanceRules( new RestrictedRuleStore( rs ) );
    }
}
