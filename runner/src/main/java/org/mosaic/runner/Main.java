package org.mosaic.runner;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class Main {

    private static final int EXIT_STATUS_SUCCESS = 0;

    private static final int EXIT_STATUS_CONFIG_ERROR = 1;

    private static final int EXIT_STATUS_START_ERROR = 2;

    private static final int EXIT_STATUS_RESTART = 3;

    private static final int EXIT_STATUS_RUN_ERROR = 4;

    private static final int EXIT_STATUS_INTERRUPTED = 5;

    private static final int EXIT_STATUS_UNKNOWN = 6;

    private static Logger logger;

    private static Map<String, String> felixConfiguration = new HashMap<String, String>();

    private static Felix felix;

    public static Path mosaicHome;

    public static Path etcDir;

    public static Path workDir;

    public static void main( String[] args ) {
        configure();
        start();
        run();
    }

    private static void configure() {
        //
        // setup Mosaic home directories
        //
        Path userDir = Paths.get( System.getProperty( "user.dir" ) );
        mosaicHome = userDir.resolve( Paths.get( System.getProperty( "mosaicHome", "mosaic" ) ) );
        if( Files.notExists( mosaicHome ) ) {
            System.err.println( "Could not find Mosaic home at: " + mosaicHome );
            System.exit( EXIT_STATUS_CONFIG_ERROR );
        }
        etcDir = mosaicHome.resolve( "etc" );
        workDir = mosaicHome.resolve( "work" );

        //
        // configure logging
        //
        Path logbackFile = etcDir.resolve( Paths.get( "logback.xml" ) );
        if( Files.notExists( logbackFile ) ) {
            System.err.println( "Could not find 'logback.xml' file at: " + logbackFile );
            System.exit( EXIT_STATUS_CONFIG_ERROR );
        }
        LoggerContext lc = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            lc.reset();
            configurator.doConfigure( logbackFile.toFile() );
            StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
            logger = LoggerFactory.getLogger( Main.class );
        } catch( JoranException e ) {
            System.err.println( "logging configuration error: " + e.getMessage() );
            e.printStackTrace( System.err );
            System.exit( EXIT_STATUS_CONFIG_ERROR );
        }

        //
        // setup bundle cache
        //
        Path frameworkStorage = workDir.resolve( "bundles" );
        felixConfiguration.put( Constants.FRAMEWORK_STORAGE, frameworkStorage.toString() );
        felixConfiguration.put( Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT );
        felixConfiguration.put( BundleCache.CACHE_BUFSIZE_PROP, ( 1024 * 16 ) + "" );

        //
        // setup package delegation from the system
        //
        //org.osgi.framework.system.packages.extra
        //org.osgi.framework.bundle.parent
        //felix.bootdelegation.implicit
    }

    private static void start() {
        logger.info( "Starting Mosaic server from: {}", mosaicHome );
        try {
            felix = new Felix( felixConfiguration );
            felix.start();
        } catch( BundleException e ) {
            logger.error( "Could not start Mosaic: " + e.getMessage(), e );
            System.exit( EXIT_STATUS_START_ERROR );
        }
    }

    private static void run() {
        while( true ) {
            try {
                FrameworkEvent event = felix.waitForStop( 1000 * 60 );
                switch( event.getType() ) {
                    case FrameworkEvent.STOPPED:
                        logger.info( "Mosaic has been stopped" );
                        System.exit( EXIT_STATUS_SUCCESS );

                    case FrameworkEvent.STOPPED_UPDATE:
                        logger.info( "Mosaic system has been updated and will now restart (same JVM)" );
                        break;

                    case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                        logger.info( "Mosaic boot class-path has been modified, restarting JVM" );
                        System.exit( EXIT_STATUS_RESTART );

                    case FrameworkEvent.ERROR:
                        logger.error( "Mosaic has been stopped due to an error" );
                        System.exit( EXIT_STATUS_RUN_ERROR );

                    case FrameworkEvent.WAIT_TIMEDOUT:
                        break;

                    default:
                        logger.error( "Mosaic has been stopped due to an unknown cause (" + event.getType() + ")" );
                        System.exit( EXIT_STATUS_UNKNOWN );
                }

            } catch( InterruptedException e ) {
                logger.warn( "Mosaic has been interrupted - exiting", e );
                System.exit( EXIT_STATUS_INTERRUPTED );

            } catch( Exception e ) {
                logger.warn( "Mosaic has encountered an unexpected error: " + e.getMessage(), e );
                System.exit( EXIT_STATUS_RUN_ERROR );
            }
        }
    }
}
