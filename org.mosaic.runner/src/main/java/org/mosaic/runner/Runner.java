package org.mosaic.runner;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.runner.logging.BundleEventsLoggerListener;
import org.mosaic.runner.logging.FelixLogger;
import org.mosaic.runner.logging.FrameworkEventsLoggerListener;
import org.mosaic.runner.logging.ServiceEventsLoggerListener;
import org.mosaic.runner.watcher.BundlesWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

/**
 * @author arik
 */
public class Runner {

    private final MosaicHomeInternal home;

    private final Logger logger = LoggerFactory.getLogger( Main.class );

    public Runner( MosaicHomeInternal home ) {
        this.home = home;
    }

    public ExitCode run() throws SystemExitException {
        long start = currentTimeMillis();

        this.logger.info( "******************************************************************************************" );
        this.logger.info( "Starting Mosaic server" );
        this.logger.info( "    Home:           {}", this.home.getHome() );
        this.logger.info( "    Boot:           {}", this.home.getBoot() );
        this.logger.info( "    Deployments:    {}", this.home.getDeploy() );
        this.logger.info( "    Configurations: {}", this.home.getEtc() );
        this.logger.info( "    Server bundles: {}", this.home.getServer() );
        this.logger.info( "    Work directory: {}", this.home.getWork() );
        this.logger.info( "******************************************************************************************" );

        this.logger.debug( " " );
        this.logger.debug( "Creating OSGi container: Apache Felix" );
        this.logger.debug( "**********************************************************" );
        Felix felix = createFelix();

        this.logger.info( " " );
        this.logger.info( "Bootstrapping..." );
        this.logger.info( "**********************************************************" );
        BundlesWatcher bootWatcher = watch( felix, this.home.getBoot(), "Bootstrap Bundles Watcher" );
        bootstrap( felix );
        bootWatcher.start();

        this.logger.info( " " );
        this.logger.info( "Deploying server bundles" );
        this.logger.info( "**********************************************************" );
        watch( felix, this.home.getServer(), "Server Bundles Watcher" ).start();

        this.logger.info( " " );
        this.logger.info( "Deploying user bundles" );
        this.logger.info( "**********************************************************" );
        watch( felix, this.home.getDeploy(), "User Bundles Watcher" ).start();

        this.logger.info( " " );
        this.logger.info( "**********************************************************" );
        this.logger.info( "Running (initialization took {} seconds)", ( currentTimeMillis() - start ) / 1000 );
        this.logger.info( "**********************************************************" );
        this.logger.info( " " );
        return waitForOsgiContainerToStop( felix );
    }

    private Felix createFelix() throws SystemExitException {
        try {

            // build Felix configuration
            Map<String, Object> felixConfig = new HashMap<>();
            felixConfig.put( Constants.FRAMEWORK_STORAGE, home.getFelixWork().toString() );
            felixConfig.put( Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT );
            felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, ( 1024 * 16 ) + "" );
            felixConfig.put( FelixConstants.LOG_LOGGER_PROP, new FelixLogger() );
            felixConfig.put( FelixConstants.LOG_LEVEL_PROP, FelixLogger.LOG_DEBUG + "" );
            felixConfig.put( Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                             "org.slf4j;version=1.6.4," +
                             "org.slf4j.spi;version=1.6.4," +
                             "org.slf4j.helpers;version=1.6.4" );
            for( Map.Entry<String, Object> entry : felixConfig.entrySet() ) {
                this.logger.debug( "    {} = {}", entry.getKey(), entry.getValue() );
            }

            // create Felix instance and start it - it should start empty since we clean the bundles dir on startup
            Felix felix = new Felix( felixConfig );
            felix.start();

            // setup event loggers
            felix.getBundleContext().addBundleListener( new BundleEventsLoggerListener() );
            felix.getBundleContext().addFrameworkListener( new FrameworkEventsLoggerListener() );
            felix.getBundleContext().addServiceListener( new ServiceEventsLoggerListener() );

            return felix;

        } catch( Exception e ) {
            throw new SystemExitException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private BundlesWatcher watch( Felix felix, Path directory, String watcherName ) throws SystemExitException {
        try {

            // setup the bootstrap watcher and start the bootstrap bundle
            BundlesWatcher watcher = new BundlesWatcher( felix.getBundleContext(), watcherName, directory );
            watcher.scan();
            return watcher;

        } catch( Exception e ) {
            throw new SystemExitException( "Could not start '" + watcherName + "': " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private void bootstrap( Felix felix ) throws SystemExitException {
        try {

            Bundle[] bundles = felix.getBundleContext().getBundles();
            if( bundles == null ) {
                throw new IllegalStateException( "No bundles have been deployed - server is probably in an illegal state" );
            }

            // start the bootstrapper bundle which catapults the rest of the server
            Bundle bootstrapBundle = null;
            for( Bundle bundle : bundles ) {
                if( bundle.getSymbolicName().equals( "org.mosaic.server.boot" ) ) {
                    if( bootstrapBundle != null ) {
                        throw new IllegalStateException( "More than one bootstrap bundle found!" );
                    } else {
                        bootstrapBundle = bundle;
                    }
                }
            }
            if( bootstrapBundle == null ) {
                throw new IllegalStateException( "Could not find bootstrap bundle!" );
            } else {
                bootstrapBundle.start();
            }

        } catch( Exception e ) {
            throw new SystemExitException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private ExitCode waitForOsgiContainerToStop( Felix felix ) {
        while( true ) {
            try {
                FrameworkEvent event = felix.waitForStop( 1000 * 60 );
                switch( event.getType() ) {
                    case FrameworkEvent.STOPPED:

                        // framework stopped normally
                        this.logger.info( "Mosaic has been stopped" );
                        return ExitCode.SUCCESS;

                    case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:

                        // boot class-path has changed which requires a JVM restart (exit-code accordingly,
                        // shell script should pick this up and restart us)
                        this.logger.info( "Mosaic boot class-path has been modified, restarting JVM" );
                        return ExitCode.RESTART;

                    case FrameworkEvent.ERROR:

                        // framework stopped abnormally, return error exit code
                        this.logger.info( "Mosaic has been stopped due to an error" );
                        return ExitCode.RUNTIME_ERROR;

                    default:

                        // framework stopped abnormally, with an unspecified reason, return an error exit code
                        this.logger.info( "Mosaic has been stopped due to an unknown cause (" + event.getType() + ")" );
                        return ExitCode.RUNTIME_ERROR;

                    case FrameworkEvent.STOPPED_UPDATE:

                        // framework is restart in the same JVM - just log and continue
                        this.logger.info( "Mosaic system has been updated and will now restart (same JVM)" );
                        continue;

                    case FrameworkEvent.WAIT_TIMEDOUT:

                        // no-op: framework is still running - do nothing and keep looping+waiting
                        // (do not remove this or the 'switch' case will go to 'default' which will stop the JVM)

                }

            } catch( InterruptedException e ) {

                this.logger.warn( "Mosaic has been interrupted - exiting", e );
                return ExitCode.INTERRUPTED;

            }
        }
    }
}
