package org.mosaic.runner.logging;

import org.mosaic.runner.Runner;
import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author arik
 */
@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class LoggingFrameworkListener implements FrameworkListener {

    private static final String MDC_BUNDLE_KEY = "logging-osgi-bundle";

    @Override
    public void frameworkEvent( FrameworkEvent event ) {
        MDC.put( MDC_BUNDLE_KEY, BundleUtils.toString( event.getBundle() ) );
        try {
            Throwable throwable = event.getThrowable();
            String throwableMsg = throwable != null ? throwable.getMessage() : "";

            Logger logger = LoggerFactory.getLogger( "org.mosaic.osgi.framework" );
            switch( event.getType() ) {
                case FrameworkEvent.STARTED:
                    synchronized( Runner.class ) {
                        logger.info( "Started the OSGi Framework", throwable );
                    }
                    break;

                case FrameworkEvent.ERROR:
                    logger.error( "OSGi Framework error has occurred: {}", throwableMsg, throwable );
                    break;

                case FrameworkEvent.PACKAGES_REFRESHED:
                    logger.info( "Refreshed OSGi packages", throwable );
                    break;

                case FrameworkEvent.STARTLEVEL_CHANGED:
                    FrameworkStartLevel startLevel = event.getBundle().adapt( FrameworkStartLevel.class );
                    logger.info( "OSGi Framework start level has been changed to: {}", startLevel.getStartLevel(), throwable );
                    break;

                case FrameworkEvent.WARNING:
                    logger.warn( "OSGi Framework warning has occurred: {}", throwableMsg, throwable );
                    break;

                case FrameworkEvent.INFO:
                    logger.info( "OSGi Framework informational has occurred: {}", throwableMsg, throwable );
                    break;

                case FrameworkEvent.STOPPED:
                    logger.info( "Stopped the OSGi Framework", throwable );
                    break;

                case FrameworkEvent.STOPPED_UPDATE:
                    logger.info( "Restarting the OSGi Framework", throwable );
                    break;

                case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                    logger.info( "Restarting the OSGi Framework due to boot class-path modification", throwable );
                    break;
            }
        } finally {
            MDC.remove( MDC_BUNDLE_KEY );
        }
    }
}
