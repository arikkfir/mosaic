package org.mosaic.launcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.util.Arrays.asList;
import static org.mosaic.launcher.EventsLogger.printEmphasizedWarnMessage;
import static org.mosaic.launcher.Header.printHeader;
import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
public final class Mosaic
{//
    private static final Logger LOG = LoggerFactory.getLogger( Mosaic.class );

    private static final String XX_USE_SPLIT_VERIFIER = "-XX:-UseSplitVerifier";

    @Nonnull
    private final List<InitTask> tasks;

    @Nonnull
    private final MosaicConfiguration configuration;

    public Mosaic()
    {
        this( new MosaicConfigurationBuilder().build() );
    }

    public Mosaic( @Nonnull MosaicConfiguration configuration )
    {
        assertJvmSplitVerifierIsUsed();

        LOG.debug( "Creating Mosaic instance with configuration: {}", configuration );
        this.configuration = configuration;

        InitHomeTask initHomeTask = new InitHomeTask( this );
        InitLoggingTask initLoggingTask = new InitLoggingTask( this );
        InitFelixTask initFelixTask = new InitFelixTask( this );
        InitShutdownHookTask initShutdownHookTask = new InitShutdownHookTask( this );
        InitBootBundlesTask initBootBundlesTask = new InitBootBundlesTask( this, initFelixTask );
        this.tasks = asList( initHomeTask, initLoggingTask, initFelixTask, initShutdownHookTask, initBootBundlesTask );
    }

    @Nonnull
    public MosaicConfiguration getConfiguration()
    {
        return this.configuration;
    }

    public void start()
    {
        LOG.debug( "Starting Mosaic server" );
        try
        {
            printHeader( this );
            for( InitTask task : this.tasks )
            {
                task.start();
            }
        }
        catch( Throwable e )
        {
            // log error
            LOG.error( "Error starting Mosaic: {}", e.getMessage(), e );

            // stop the server
            stop();

            // re-throw error (wrapped in bootstrap exception if not already)
            if( e instanceof SystemError.BootstrapException )
            {
                throw e;
            }
            else
            {
                throw bootstrapError( "Could not start Mosaic server: {}", e.getMessage(), e );
            }
        }
    }

    public void stop()
    {
        printEmphasizedWarnMessage( "Mosaic server is stopping..." );

        List<InitTask> reversedTasks = new LinkedList<>( this.tasks );
        Collections.reverse( reversedTasks );
        for( InitTask task : reversedTasks )
        {
            try
            {
                task.stop();
            }
            catch( Throwable e )
            {
                LOG.error( "A shutdown-task failed to execute: {}", e.getMessage(), e );
            }
        }

        printEmphasizedWarnMessage( "Mosaic server is stopped" );
    }

    private void assertJvmSplitVerifierIsUsed()
    {
        LOG.debug( "Verifying JVM split-verifier is used (required for bytecode weaving)" );
        for( String arg : getRuntimeMXBean().getInputArguments() )
        {
            if( arg.contains( XX_USE_SPLIT_VERIFIER ) )
            {
                return;
            }
        }
        throw bootstrapError(
                "The JVM split verifier argument has not been specified.\n" +
                "The JVM split verifier is required to enable bytecode \n" +
                "weaving by the Mosaic server.\n" +
                "Please provide the argument to the JVM command line:\n" +
                "    java ... {} ...",
                XX_USE_SPLIT_VERIFIER
        );
    }
}
