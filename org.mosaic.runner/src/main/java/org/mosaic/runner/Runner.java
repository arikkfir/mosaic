package org.mosaic.runner;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.runner.deploy.lifecycle.BundleDeployer;
import org.mosaic.runner.deploy.watcher.BundlesWatcher;
import org.mosaic.runner.logging.BundleEventsLoggerListener;
import org.mosaic.runner.logging.FelixLogger;
import org.mosaic.runner.logging.FrameworkEventsLoggerListener;
import org.mosaic.runner.logging.ServiceEventsLoggerListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class Runner {

    private final MosaicHome home;

    private final Logger logger = LoggerFactory.getLogger( Main.class );

    private Felix felix;

    @Inject
    public Runner( MosaicHome home ) throws StartException {
        this.home = home;
    }

    public ExitCode run() throws StartException {
        startFelix();
        return waitForOsgiContainerToStop();
    }

    private void startFelix() throws StartException {
        this.logger.debug( "Starting Apache Felix" );
        try {
            //
            // build Felix configuration
            //
            Map<String, Object> felixConfig = new HashMap<>();
            felixConfig.put( Constants.FRAMEWORK_STORAGE, home.getFelixWork().toString() );
            felixConfig.put( Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT );
            felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, ( 1024 * 16 ) + "" );
            felixConfig.put( FelixConstants.LOG_LOGGER_PROP, new FelixLogger() );
            felixConfig.put( FelixConstants.LOG_LEVEL_PROP, FelixLogger.LOG_DEBUG + "" );

            //
            // create Felix instance and start it - it should start empty since we're cleaning the bundles
            // directory on startup
            //
            this.felix = new Felix( felixConfig );
            this.felix.start();

            //
            // setup event loggers
            //
            BundleContext systemContext = this.felix.getBundleContext();
            systemContext.addBundleListener( new BundleEventsLoggerListener() );
            systemContext.addFrameworkListener( new FrameworkEventsLoggerListener() );
            systemContext.addServiceListener( new ServiceEventsLoggerListener() );
            systemContext.addBundleListener( new BundleDeployer() );

            //
            // setup bundle deployment watchers
            //
            new BundlesWatcher( systemContext, this.home.getServer() ).start( "ServerBundlesWatcher" );
            new BundlesWatcher( systemContext, this.home.getDeploy() ).start( "UserBundlesWatcher" );

        } catch( Exception e ) {
            throw new StartException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e );
        }
    }

    private ExitCode waitForOsgiContainerToStop() {
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
