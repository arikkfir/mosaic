package org.mosaic.runner;

import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
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
        this.logger.info( " Starting Mosaic server" );
        this.logger.info( "    Home:           {}", this.home.getHome() );
        this.logger.info( "    Boot:           {}", this.home.getBoot() );
        this.logger.info( "    Deployments:    {}", this.home.getDeploy() );
        this.logger.info( "    Configurations: {}", this.home.getEtc() );
        this.logger.info( "    Server bundles: {}", this.home.getServer() );
        this.logger.info( "    Work directory: {}", this.home.getWork() );
        this.logger.info( "******************************************************************************************" );
        this.logger.info( " " );

        Felix felix = createFelix();
        watch( felix );
        bootstrap( felix );

        // print summary and wait for the server to shutdown
        this.logger.info( " " );
        this.logger.info( "*************************************************************************" );
        long startupDurationMillis = currentTimeMillis() - start;
        this.logger.info( " Running (initialization took {} seconds, or {} milli-seconds)", startupDurationMillis / 1000, startupDurationMillis );
        this.logger.info( "*************************************************************************" );
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
                             "javax.annotation;version=1.7.0," +
                             "javax.management;version=1.7.0," +
                             "javax.naming;version=1.7.0," +
                             "javax.naming.spi;version=1.7.0," +
                             "javax.sql;version=1.7.0," +
                             "javax.xml.parsers;version=1.7.0," +
                             "org.w3c.dom;version=1.7.0," +
                             "sun.misc;version=1.7.0," +
                             "org.slf4j;version=1.6.4," +
                             "org.slf4j.spi;version=1.6.4," +
                             "org.slf4j.helpers;version=1.6.4" );

            // create Felix instance and start it - it should start empty since we clean the bundles dir on startup
            Felix felix = new Felix( felixConfig );
            felix.start();

            // setup event loggers
            felix.getBundleContext().addFrameworkListener( new FrameworkEventsLoggerListener() );
            felix.getBundleContext().addServiceListener( new ServiceEventsLoggerListener() );

            return felix;

        } catch( Exception e ) {
            throw new SystemExitException( "Could not start OSGi container (Apache Felix): " + e.getMessage(), e, ExitCode.START_ERROR );
        }
    }

    private void watch( Felix felix ) throws SystemExitException {
        try {

            BundlesWatcher watcher = new BundlesWatcher( felix.getBundleContext(),
                                                         this.home.getHome().resolve( "pause" ),
                                                         this.home.getBoot(),
                                                         this.home.getServer(),
                                                         this.home.getDeploy() );
            watcher.scan();
            watcher.start();

        } catch( Exception e ) {
            throw new SystemExitException( "Could not start bundles watcher: " + e.getMessage(), e, ExitCode.START_ERROR );
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
