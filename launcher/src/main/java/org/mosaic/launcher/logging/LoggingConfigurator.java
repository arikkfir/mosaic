package org.mosaic.launcher.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.StatusPrinter;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.launcher.MosaicInstance;
import org.mosaic.launcher.util.SystemError;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static ch.qos.logback.core.status.StatusUtil.filterStatusListByTimeThreshold;
import static java.nio.file.Files.exists;
import static org.mosaic.launcher.util.SystemError.bootstrapError;
import static org.mosaic.launcher.util.Utils.requireClasspathResource;

/**
 * @author arik
 */
public class LoggingConfigurator
{
    @Nonnull
    private final MosaicInstance mosaic;

    public LoggingConfigurator( @Nonnull MosaicInstance mosaic )
    {
        this.mosaic = mosaic;
    }

    public void initializeLogging()
    {
        try
        {
            // obtain logger context from Logback
            LoggerContext lc = ( LoggerContext ) LoggerFactory.getILoggerFactory();
            lc.reset();

            // disable logback packaging source calculation (causes problems when bundles disappear, on felix shutdown, etc)
            lc.setPackagingDataEnabled( false );

            // apply our properties on the logger context so we can use them in logback*.xml files
            for( String propertyName : this.mosaic.getProperties().stringPropertyNames() )
            {
                lc.putProperty( propertyName, this.mosaic.getProperties().getProperty( propertyName ) );
            }

            // apply built-in & user-customizable configurations
            AppenderRegistry appenderRegistry = new AppenderRegistry();
            applyBuiltinLogbackConfiguration( lc, appenderRegistry );
            applyServerLogbackConfiguration( lc, appenderRegistry );

            // install JUL-to-SLF4J adapter
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
        catch( SystemError.BootstrapException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not initialize Mosaic logging framework: {}", e.getMessage(), e );
        }
    }

    private void applyBuiltinLogbackConfiguration( @Nonnull LoggerContext lc,
                                                   @Nonnull org.mosaic.launcher.logging.AppenderRegistry appenderRegistry )
    {
        JoranConfigurator configurator = new LogbackBuiltinConfigurator( appenderRegistry );
        configurator.setContext( lc );
        try
        {
            configurator.doConfigure( requireClasspathResource( this.mosaic.getProperties(), "logbackBuiltin", "/logback-builtin.xml" ) );
            checkLogbackContextForErrors( lc );
        }
        catch( JoranException e )
        {
            throw bootstrapError( "Error while applying built-in Logback configuration: {}", e.getMessage(), e );
        }
    }

    private void applyServerLogbackConfiguration( @Nonnull LoggerContext lc,
                                                  @Nonnull AppenderRegistry appenderRegistry )
    {
        Path logbackConfigFile = this.mosaic.getEtc().resolve( "logback.xml" );
        if( exists( logbackConfigFile ) )
        {
            try
            {
                LogbackRestrictedConfigurator configurator = new LogbackRestrictedConfigurator( appenderRegistry );
                configurator.setContext( lc );
                configurator.doConfigure( logbackConfigFile.toFile() );
                checkLogbackContextForErrors( lc );
            }
            catch( JoranException e )
            {
                throw SystemError.bootstrapError( "Error while applying Logback configuration in '{}': {}", logbackConfigFile, e.getMessage(), e );
            }
        }
    }

    private void checkLogbackContextForErrors( @Nonnull LoggerContext lc )
    {
        if( new StatusChecker( lc ).getHighestLevel( 0 ) >= ErrorStatus.WARN )
        {
            System.out.println();
            System.out.printf( "LOGGING CONFIGURATION ERRORS DETECTED:\n" );
            System.out.println();
            System.out.println();
            StatusPrinter.printInCaseOfErrorsOrWarnings( lc );

            StringBuilder sb = new StringBuilder();
            for( Status s : filterStatusListByTimeThreshold( lc.getStatusManager().getCopyOfStatusList(), 0 ) )
            {
                StatusPrinter.buildStr( sb, "", s );
            }

            throw SystemError.bootstrapError( "LOGGING CONFIGURATION ERRORS DETECTED:\n" + sb );
        }
    }
}
