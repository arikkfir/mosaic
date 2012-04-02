package org.mosaic.runner.logging;

import org.mosaic.runner.util.BundleUtils;
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
        String bundle = BundleUtils.toString( event.getBundle() );
        switch( event.getType() ) {
            case BundleEvent.INSTALLED:
                String origin = BundleUtils.toString( event.getOrigin() );
                this.logger.info( "Bundle '{}' has been installed (by bundle '{}')", bundle, origin );
                break;

            case BundleEvent.RESOLVED:
                this.logger.info( "Bundle '{}' has been resolved", bundle );
                break;

            case BundleEvent.STARTING:
                this.logger.info( "Bundle '{}' is starting", bundle );
                break;

            case BundleEvent.LAZY_ACTIVATION:
                this.logger.info( "Bundle '{}' will be activated lazily", bundle );
                break;

            case BundleEvent.STARTED:
                this.logger.info( "Bundle '{}' has been started", bundle );
                break;

            case BundleEvent.UPDATED:
                this.logger.info( "Bundle '{}' has been updated", bundle );
                break;

            case BundleEvent.STOPPING:
                this.logger.info( "Bundle '{}' is stopping", bundle );
                break;

            case BundleEvent.STOPPED:
                this.logger.info( "Bundle '{}' has been stopped", bundle );
                break;

            case BundleEvent.UNRESOLVED:
                this.logger.info( "Bundle '{}' has been unresolved", bundle );
                break;

            case BundleEvent.UNINSTALLED:
                this.logger.info( "Bundle '{}' has been uninstalled", bundle );
                break;
        }
    }
}
