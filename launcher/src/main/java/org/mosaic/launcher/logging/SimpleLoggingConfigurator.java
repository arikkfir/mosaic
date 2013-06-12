package org.mosaic.launcher.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import java.util.Properties;
import javax.annotation.Nonnull;

import static org.mosaic.launcher.util.Utils.requireClasspathResource;

/**
 * @author arik
 */
public class SimpleLoggingConfigurator extends AbstractLoggingConfigurator
{
    public SimpleLoggingConfigurator( @Nonnull Properties properties )
    {
        super( properties );
    }

    @Override
    protected void configureLoggerContext( @Nonnull LoggerContext loggerContext ) throws Exception
    {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext( loggerContext );
        configurator.doConfigure( requireClasspathResource( this.properties, "logbackDefault", "/logback.xml" ) );
    }
}
