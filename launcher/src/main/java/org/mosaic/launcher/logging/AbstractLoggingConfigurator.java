package org.mosaic.launcher.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.StatusPrinter;
import java.util.Properties;
import javax.annotation.Nonnull;
import org.mosaic.launcher.util.SystemError;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static ch.qos.logback.core.status.StatusUtil.filterStatusListByTimeThreshold;

/**
 * @author arik
 */
public abstract class AbstractLoggingConfigurator
{
    @Nonnull
    protected final Properties properties;

    public AbstractLoggingConfigurator( @Nonnull Properties properties )
    {
        this.properties = properties;
    }

    public final void initializeLogging() throws Exception
    {
        LoggerContext loggerContext = getAndResetLoggerContext();
        configureLoggerContext( loggerContext );
        checkLogbackContextForErrors( loggerContext );

        // install JUL-to-SLF4J adapter
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    protected abstract void configureLoggerContext( @Nonnull LoggerContext loggerContext ) throws Exception;

    private LoggerContext getAndResetLoggerContext()
    {
        // obtain and reset logger context from Logback
        LoggerContext lc = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        lc.reset();

        // disable logback packaging source calculation (causes problems when bundles disappear, on felix shutdown, etc)
        lc.setPackagingDataEnabled( false );

        // apply our properties on the logger context so we can use them in logback*.xml files
        for( String propertyName : this.properties.stringPropertyNames() )
        {
            lc.putProperty( propertyName, this.properties.getProperty( propertyName ) );
        }
        return lc;
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
