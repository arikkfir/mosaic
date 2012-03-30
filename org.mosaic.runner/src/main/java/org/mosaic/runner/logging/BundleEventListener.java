package org.mosaic.runner.logging;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class BundleEventListener implements BundleListener {

    private final Logger logger;

    public BundleEventListener() {
        this.logger = LoggerFactory.getLogger( "org.osgi.bundles" );
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        String bsn = bundle.getSymbolicName();
        Version version = bundle.getVersion();
        long bundleId = bundle.getBundleId();

        Bundle origin = event.getOrigin();
        String originBsn = bundle.getSymbolicName();
        Version originVersion = bundle.getVersion();
        long originBundleId = bundle.getBundleId();

        switch( event.getType() ) {
            case BundleEvent.INSTALLED:
                if( origin != bundle ) {
                    this.logger.info( "Bundle '{}-{}[{}]' has been installed (by bundle '{}-{}[{}]')",
                                      new Object[] {
                                              bsn,
                                              version,
                                              bundleId,
                                              originBsn,
                                              originVersion,
                                              originBundleId
                                      } );
                } else {
                    this.logger.info( "Bundle '{}-{}[{}]' has been installed",
                                      new Object[] {
                                              bsn,
                                              version,
                                              bundleId
                                      } );
                }
                break;

            case BundleEvent.RESOLVED:
                this.logger.info( "Bundle '{}-{}[{}]' has been resolved",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.STARTING:
                this.logger.info( "Bundle '{}-{}[{}]' is starting",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.LAZY_ACTIVATION:
                this.logger.info( "Bundle '{}-{}[{}]' will be activated lazily",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.STARTED:
                this.logger.info( "Bundle '{}-{}[{}]' has been started",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.UPDATED:
                this.logger.info( "Bundle '{}-{}[{}]' has been updated",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.STOPPING:
                this.logger.info( "Bundle '{}-{}[{}]' is stopping",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.STOPPED:
                this.logger.info( "Bundle '{}-{}[{}]' has been stopped",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.UNRESOLVED:
                this.logger.info( "Bundle '{}-{}[{}]' has been unresolved",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;

            case BundleEvent.UNINSTALLED:
                this.logger.info( "Bundle '{}-{}[{}]' has been uninstalled",
                                  new Object[] {
                                          bsn,
                                          version,
                                          bundleId
                                  } );
                break;
        }
    }
}
