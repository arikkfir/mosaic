package org.mosaic.runner.logging;

import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author arik
 */
public class LoggingBundleListener implements SynchronousBundleListener
{
    private static final String OSGI_LOG_NAME = "org.mosaic.server.osgi.bundles";

    private static final String MDC_BUNDLE_KEY = "logging-osgi-bundle";

    @Override
    public void bundleChanged( BundleEvent event )
    {
        Bundle bundle = event.getBundle();
        String bts = BundleUtils.toString( bundle );

        MDC.put( MDC_BUNDLE_KEY, bts );
        try
        {
            Logger logger = LoggerFactory.getLogger( OSGI_LOG_NAME );
            switch( event.getType() )
            {
                case BundleEvent.INSTALLED:
                    logger.info( "Installed bundle: {}", bts );
                    break;

                case BundleEvent.RESOLVED:
                    logger.info( "Resolved bundle: {}", bts );
                    break;

                case BundleEvent.STARTING:
                    logger.debug( "Starting bundle: {}", bts );
                    break;

                case BundleEvent.STARTED:
                    logger.info( "Started bundle: {}", bts );
                    break;

                case BundleEvent.UPDATED:
                    logger.info( "Updated bundle: {}", bts );
                    break;

                case BundleEvent.STOPPING:
                    logger.debug( "Stopping bundle: {}", bts );
                    break;

                case BundleEvent.STOPPED:
                    logger.info( "Stopped bundle: {}", bts );
                    break;

                case BundleEvent.UNRESOLVED:
                    logger.info( "Unresolved bundle: {}", bts );
                    break;

                case BundleEvent.UNINSTALLED:
                    logger.info( "Uninstalled bundle: {}", bts );
                    break;
            }
        }
        finally
        {
            MDC.remove( MDC_BUNDLE_KEY );
        }
    }
}
