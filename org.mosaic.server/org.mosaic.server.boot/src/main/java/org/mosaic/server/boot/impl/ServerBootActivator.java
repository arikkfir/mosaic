package org.mosaic.server.boot.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.track.BundleTracker;
import org.mosaic.server.boot.impl.track.OsgiSpringNamespacePlugin;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * @author arik
 */
public class ServerBootActivator implements BundleActivator, BundleListener {

    private static final Logger LOG = LoggerFactory.getLogger( ServerBootActivator.class );

    private Map<Long, BundleTracker> bundleTrackers;

    private OsgiSpringNamespacePlugin osgiSpringNamespacePlugin;

    @Override
    public void start( BundleContext bundleContext ) throws Exception {
        this.bundleTrackers = new ConcurrentHashMap<>( 100 );
        this.osgiSpringNamespacePlugin = new OsgiSpringNamespacePlugin( bundleContext );
        bundleContext.addBundleListener( this.osgiSpringNamespacePlugin );
        bundleContext.addBundleListener( this );
    }

    @Override
    public void stop( BundleContext bundleContext ) throws Exception {
        bundleContext.removeBundleListener( this );
        bundleContext.removeBundleListener( this.osgiSpringNamespacePlugin );
        for( BundleTracker tracker : this.bundleTrackers.values() ) {
            tracker.stop();
        }
        this.bundleTrackers = null;
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Map<Long, BundleTracker> trackers = this.bundleTrackers;
        if( trackers == null ) {

            // event received after this bootstrapper was closed - ignore the event
            return;

        }

        Bundle bundle = event.getBundle();
        if( isMosaicBundle( bundle ) ) {

            if( event.getType() == BundleEvent.INSTALLED ) {

                // first attempt to resolve it - if successful, a RESOLVE event will be fired which will invoke us again
                // and that's where we'll start to actually track the bundle - here we'll just resolve it and return
                Bundle systemBundle = bundle.getBundleContext().getBundle( 0 );
                FrameworkWiring frameworkWiring = systemBundle.adapt( FrameworkWiring.class );
                if( frameworkWiring != null ) {
                    frameworkWiring.resolveBundles( Arrays.asList( bundle ) );
                } else {
                    LOG.warn( "Could not resolve bundle '{}' - the OSGi framework wiring service is not available", BundleUtils.toString( bundle ) );
                }

            } else if( event.getType() == BundleEvent.RESOLVED ) {

                // track this new mosaic bundle
                BundleTracker tracker = new BundleTracker( bundle, this.osgiSpringNamespacePlugin );
                tracker.start();
                trackers.put( bundle.getBundleId(), tracker );

            } else if( event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED ) {

                // stop tracking this mosaic bundle
                BundleTracker tracker = trackers.remove( bundle.getBundleId() );
                if( tracker != null ) {
                    tracker.stop();
                }

            }
        }
    }

    private static boolean isMosaicBundle( Bundle bundle ) {
        if( bundle.getEntryPaths( "/META-INF/mosaic/" ) == null ) {

            // all mosaic bundles must have the 'Mosaic-Bundle' header
            return false;

        } else if( bundle.getHeaders( bundle.getHeaders().get( Constants.BUNDLE_ACTIVATOR ) ) != null ) {

            // mosaic bundles must not have activators
            LOG.warn( "Bundle '{}' is a Mosaic bundle, but also has a 'Bundle-Activator' header; Mosaic bundles must have no activator, and therefor will be ignored and treated as a standard bundle.", BundleUtils.toString( bundle ) );
            return false;

        } else {

            // bundle has the mosaic bundle header, and has no activator - approved
            return true;

        }
    }
}
