package org.mosaic.launcher.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import java.nio.file.Path;
import java.util.Properties;
import javax.annotation.Nonnull;

import static java.nio.file.Files.exists;
import static org.mosaic.launcher.util.Utils.requireClasspathResource;

/**
 * @author arik
 */
public class ServerLoggingConfigurator extends AbstractLoggingConfigurator
{
    @Nonnull
    private final Path serverEtcPath;

    public ServerLoggingConfigurator( @Nonnull Properties properties, @Nonnull Path serverEtcPath )
    {
        super( properties );
        this.serverEtcPath = serverEtcPath;
    }

    @Override
    protected void configureLoggerContext( @Nonnull LoggerContext loggerContext ) throws Exception
    {
        AppenderRegistry appenderRegistry = new AppenderRegistry();

        JoranConfigurator serverConfigurator = new LogbackBuiltinConfigurator( appenderRegistry );
        serverConfigurator.setContext( loggerContext );
        serverConfigurator.doConfigure( requireClasspathResource( this.properties, "logbackServer", "/logback-builtin.xml" ) );

        Path logbackConfigFile = this.serverEtcPath.resolve( "logback.xml" );
        if( exists( logbackConfigFile ) )
        {
            LogbackRestrictedConfigurator userConfigurator = new LogbackRestrictedConfigurator( appenderRegistry );
            userConfigurator.setContext( loggerContext );
            userConfigurator.doConfigure( logbackConfigFile.toFile() );
        }
    }
}
