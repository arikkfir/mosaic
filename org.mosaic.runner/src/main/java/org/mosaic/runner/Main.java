package org.mosaic.runner;

import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.runner.exit.ExitCode;
import org.mosaic.runner.exit.StartException;
import org.mosaic.runner.exit.SystemExitException;
import org.mosaic.runner.logging.BundleEventListener;
import org.mosaic.runner.logging.FelixLogger;
import org.mosaic.runner.logging.FrameworkEventListener;
import org.mosaic.runner.logging.ServiceEventListener;
import org.mosaic.runner.watcher.BundlesWatcher;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class Main {

    private static MosaicHome home;

    private static Logger logger;

    private static Felix felix;

    public static void main( String[] args ) {
        try {
            home = new MosaicHome();
            logger = LoggerFactory.getLogger( Main.class );

            startFelix();

            ExitCode exitCode = run();
            System.exit( exitCode.getCode() );

        } catch( SystemExitException e ) {
            e.printStackTrace( System.err );
            System.exit( e.getExitCode().getCode() );

        } catch( Exception e ) {
            e.printStackTrace( System.err );
            System.exit( ExitCode.UNKNOWN_ERROR.getCode() );
        }
    }

    private static void startFelix() throws StartException {
        logger.debug( "Starting Apache Felix" );
        try {
            Map<String, Object> felixConfig = new HashMap<>();
            felixConfig.put( Constants.FRAMEWORK_STORAGE, home.getFelixWork().toString() );
            felixConfig.put( Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT );
            felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, ( 1024 * 16 ) + "" );
            felixConfig.put( FelixConstants.LOG_LOGGER_PROP, new FelixLogger() );
            felixConfig.put( FelixConstants.LOG_LEVEL_PROP, FelixLogger.LOG_DEBUG + "" );

            felix = new Felix( felixConfig );
            felix.start();
            felix.getBundleContext().addBundleListener( new BundleEventListener() );
            felix.getBundleContext().addServiceListener( new ServiceEventListener() );
            felix.getBundleContext().addFrameworkListener( new FrameworkEventListener() );

            new BundlesWatcher( logger, felix, 2, 500, home.getServer() ).start( "ServerBundlesWatcher" );

        } catch( Exception e ) {
            throw new StartException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e );
        }
    }

    private static ExitCode run() {
        while( true ) {
            try {
                FrameworkEvent event = felix.waitForStop( 1000 * 60 );
                switch( event.getType() ) {
                    case FrameworkEvent.STOPPED:
                        //
                        // framework stopped normally
                        //
                        logger.info( "Mosaic has been stopped" );
                        return ExitCode.SUCCESS;

                    case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                        //
                        // boot class-path has changed which requires a JVM restart (exit-code accordingly,
                        // shell script should pick this up and restart us)
                        //
                        logger.info( "Mosaic boot class-path has been modified, restarting JVM" );
                        return ExitCode.RESTART;

                    case FrameworkEvent.ERROR:
                        //
                        // framework stopped abnormally, return error exit code
                        //
                        logger.info( "Mosaic has been stopped due to an error" );
                        return ExitCode.RUNTIME_ERROR;

                    default:
                        //
                        // framework stopped abnormally, with an unspecified reason, return an error exit code
                        //
                        logger.info( "Mosaic has been stopped due to an unknown cause (" + event.getType() + ")" );
                        return ExitCode.RUNTIME_ERROR;

                    case FrameworkEvent.STOPPED_UPDATE:
                        //
                        // framework is restart in the same JVM - just log and continue
                        //
                        logger.info( "Mosaic system has been updated and will now restart (same JVM)" );
                        continue;

                    case FrameworkEvent.WAIT_TIMEDOUT:
                        //
                        // no-op: framework is still running - do nothing and keep looping+waiting
                        // (do not remove this or the 'switch' case will go to 'default' which will stop the JVM)
                        //
                }

            } catch( InterruptedException e ) {
                logger.warn( "Mosaic has been interrupted - exiting", e );
                return ExitCode.INTERRUPTED;
            }
        }
    }
}
