package org.mosaic.launcher;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * @author arik
 */
final class InitLoggingTask extends InitTask
{
    InitLoggingTask( @Nonnull Mosaic mosaic )
    {
        super( mosaic );
    }

    @Override
    public void start()
    {
        this.log.debug( "Initializing logging framework" );
        this.log.debug( "Connecting 'java.util.logging' to Logback" );
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LoggerContext loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();

        Path overlayLogbackFile = getEtc().resolve( "logback.xml" );
        if( Files.exists( overlayLogbackFile ) )
        {
            try
            {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext( loggerContext );
                configurator.doConfigure( overlayLogbackFile.toFile() );
            }
            catch( JoranException je )
            {
                // errors will be printed anyway by the StatusPrinter call below
            }
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings( loggerContext );
    }

    @Override
    public void stop()
    {
        // no-op
    }
}
