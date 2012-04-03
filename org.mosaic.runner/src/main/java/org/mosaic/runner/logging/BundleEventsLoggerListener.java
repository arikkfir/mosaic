package org.mosaic.runner.logging;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class BundleEventsLoggerListener implements BundleListener {

    private final Logger logger = LoggerFactory.getLogger( "org.osgi.bundles" );

    @Override
    public void bundleChanged( BundleEvent event ) {
        String bundle = LogUtils.toString( event.getBundle() );
        switch( event.getType() ) {
            case BundleEvent.INSTALLED:
                String origin = LogUtils.toString( event.getOrigin() );
                this.logger.info( "Installed bundle '{}' (installer was bundle '{}')", bundle, origin );
                break;

            case BundleEvent.RESOLVED:
                this.logger.info( "Resolved bundle '{}'", bundle );
                break;

            case BundleEvent.STARTING:
                this.logger.debug( "Bundle '{}' is starting", bundle );
                break;

            case BundleEvent.LAZY_ACTIVATION:
                this.logger.debug( "Bundle '{}' will be activated lazily", bundle );
                break;

            case BundleEvent.STARTED:
                this.logger.info( "Started bundle '{}'", bundle );
                break;

            case BundleEvent.UPDATED:
                this.logger.info( "Updated bundle '{}'", bundle );
                break;

            case BundleEvent.STOPPING:
                this.logger.debug( "Bundle '{}' is stopping", bundle );
                break;

            case BundleEvent.STOPPED:
                this.logger.info( "Stopped bundle '{}'", bundle );
                break;

            case BundleEvent.UNRESOLVED:
                this.logger.info( "Unresolved bundle '{}'", bundle );
                break;

            case BundleEvent.UNINSTALLED:
                this.logger.info( "Uninstalled bundle '{}'", bundle );
                break;
        }
    }
}
