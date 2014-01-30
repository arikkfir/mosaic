package org.mosaic.launcher;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nonnull;
import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.util.Arrays.asList;
import static org.mosaic.launcher.EventsLogger.printEmphasizedWarnMessage;
import static org.mosaic.launcher.Header.printHeader;
import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
public final class Mosaic
{
    private static final Logger LOG = LoggerFactory.getLogger( Mosaic.class );

    private static final String XX_USE_SPLIT_VERIFIER = "-XX:-UseSplitVerifier";

    private static final boolean devMode = Boolean.getBoolean( "devMode" ) || Boolean.getBoolean( "dev" );

    @Nonnull
    private static final String version;

    private static final Path home = Paths.get( System.getProperty( "mosaic.home", System.getProperty( "user.dir" ) ) );

    @Nonnull
    private static final Path apps;

    @Nonnull
    private static final Path etc;

    @Nonnull
    private static final Path lib;

    @Nonnull
    private static final Path logs;

    @Nonnull
    private static final Path work;

    @Nonnull
    private static final InitFelixTask felixTask;

    @Nonnull
    private static final List<InitTask> tasks;

    static
    {
        // mark launch time
        System.setProperty( "mosaic.launch.start", System.currentTimeMillis() + "" );

        // connect SLF4J to java.util.logging
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // read mosaic.properties & initialize version
        URL mosaicPropertiesResource = Mosaic.class.getResource( "mosaic.properties" );
        if( mosaicPropertiesResource == null )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not find 'mosaic.properties' file." );
        }
        Properties mosaicVersionProperties = new Properties();
        try( InputStream input = mosaicPropertiesResource.openStream() )
        {
            mosaicVersionProperties.load( input );
            version = mosaicVersionProperties.getProperty( "mosaic.version" );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not read from '{}': {}", mosaicPropertiesResource, e.getMessage(), e );
        }

        // initialize home directory locations
        apps = home.resolve( "apps" );
        etc = home.resolve( "etc" );
        lib = home.resolve( "lib" );
        logs = home.resolve( "logs" );
        work = home.resolve( "work" );

        // initialize tasks
        InitHomeTask initHomeTask = new InitHomeTask();
        InitFelixTask initFelixTask = new InitFelixTask();
        InitShutdownHookTask initShutdownHookTask = new InitShutdownHookTask();
        tasks = asList( initHomeTask, initFelixTask, initShutdownHookTask );

        felixTask = initFelixTask;
    }

    @Nonnull
    public static BundleContext getBundleContext()
    {
        Felix felix = felixTask.getFelix();
        if( felix == null )
        {
            throw new IllegalStateException( "OSGi container has not been created yet" );
        }

        BundleContext bundleContext = felix.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "OSGi container has no bundle context" );
        }

        return bundleContext;
    }

    public static boolean isDevMode()
    {
        return devMode;
    }

    @Nonnull
    public static String getVersion()
    {
        return version;
    }

    public static Path getHome()
    {
        return home;
    }

    @Nonnull
    public static Path getApps()
    {
        return apps;
    }

    @Nonnull
    public static Path getEtc()
    {
        return etc;
    }

    @Nonnull
    public static Path getLib()
    {
        return lib;
    }

    @Nonnull
    public static Path getLogs()
    {
        return logs;
    }

    @Nonnull
    public static Path getWork()
    {
        return work;
    }

    static void start()
    {
        LOG.debug( "Starting Mosaic server" );
        try
        {
            printHeader();
            for( InitTask task : tasks )
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

    static void stop()
    {
        printEmphasizedWarnMessage( "Mosaic server is stopping..." );

        List<InitTask> reversedTasks = new LinkedList<>( tasks );
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

    public static void main( String[] args )
    {
        assertJvmSplitVerifierIsUsed();

        // install an exception handler for all threads that don't have an exception handler, that simply logs the exception
        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException( Thread t, Throwable e )
            {
                LOG.error( e.getMessage(), e );
            }
        } );

        // start mosaic
        start();
    }

    private static void assertJvmSplitVerifierIsUsed()
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
