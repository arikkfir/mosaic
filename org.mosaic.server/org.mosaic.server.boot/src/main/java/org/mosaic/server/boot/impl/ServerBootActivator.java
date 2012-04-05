package org.mosaic.server.boot.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.*;

/**
 * @author arik
 */
public class ServerBootActivator implements BundleActivator, BundleListener {

    private static final Logger LOG = LoggerFactory.getLogger( ServerBootActivator.class );

    private Map<Long, BundlePublisher> bundleTrackers;

    private OsgiSpringNamespacePlugin springNamespacePlugin;

    @Override
    public void start( BundleContext bundleContext ) throws Exception {
        this.bundleTrackers = new ConcurrentHashMap<>( 100 );

        this.springNamespacePlugin = new OsgiSpringNamespacePlugin( bundleContext );
        bundleContext.addBundleListener( this.springNamespacePlugin );

        bundleContext.addBundleListener( this );
    }

    @Override
    public void stop( BundleContext bundleContext ) throws Exception {
        for( BundlePublisher publisher : this.bundleTrackers.values() ) {
            publisher.stop();
        }

        bundleContext.removeBundleListener( this );

        bundleContext.removeBundleListener( this.springNamespacePlugin );
        this.springNamespacePlugin = null;

        this.bundleTrackers = null;
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Map<Long, BundlePublisher> trackers = this.bundleTrackers;
        if( trackers == null ) {

            // event received after this bootstrapper was closed - ignore the event
            return;

        }

        Bundle bundle = event.getBundle();
        if( event.getType() == BundleEvent.INSTALLED ) {

            // if new bundle - just start it and return; also ensure it's still in INSTALLED state as events might arrive late
            // (it's the STARTED event that we really want and where we'll possibly track the bundle)
            if( bundle.getState() == Bundle.INSTALLED ) {
                try {
                    bundle.start();
                } catch( BundleException e ) {
                    LOG.warn( "Could not start bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                }
            }

        } else if( event.getType() == BundleEvent.STARTED ) {

            // only track mosaic bundles
            if( shouldTrackBundle( bundle ) ) {

                BundlePublisher publisher = new BundlePublisher( bundle, this.springNamespacePlugin );
                try {
                    publisher.start();
                    trackers.put( bundle.getBundleId(), publisher );
                } catch( Exception e ) {
                    LOG.error( "Cannot track bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                }

            }

        } else if( event.getType() == BundleEvent.STOPPED ) {

            // stop tracking this mosaic bundle
            BundlePublisher publisher = trackers.remove( bundle.getBundleId() );
            if( publisher != null ) {
                try {
                    publisher.stop();
                } catch( Exception e ) {
                    LOG.error( "An error occurred while removing bundle '{}' from the list of tracked bundles: {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                }
            }

        }
    }

    private static boolean shouldTrackBundle( Bundle bundle ) {
        if( bundle.getEntryPaths( "/META-INF/spring/" ) == null ) {

            // all mosaic bundles must have the 'Mosaic-Bundle' header
            return false;

        } else if( bundle.getHeaders().get( Constants.BUNDLE_ACTIVATOR ) != null ) {

            // mosaic bundles must not have activators
            LOG.warn( "Bundle '{}' is a Mosaic bundle, but also has a 'Bundle-Activator' header; Mosaic bundles must have no activator, and therefor will be ignored and treated as a standard bundle.", BundleUtils.toString( bundle ) );
            return false;

        } else {

            // bundle has the mosaic bundle header, and has no activator - approved
            return true;

        }
    }
}
