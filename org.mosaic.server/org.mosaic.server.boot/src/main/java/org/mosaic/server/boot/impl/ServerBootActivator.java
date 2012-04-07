package org.mosaic.server.boot.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.MosaicHome;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * @author arik
 */
public class ServerBootActivator implements BundleActivator, SynchronousBundleListener {

    private static final Logger LOG = LoggerFactory.getLogger( ServerBootActivator.class );

    private BundleContext bundleContext;

    private Map<Long, BundlePublisher> bundleTrackers;

    private OsgiSpringNamespacePlugin springNamespacePlugin;

    @Override
    public void start( BundleContext bundleContext ) throws Exception {
        this.bundleContext = bundleContext;

        bundleContext.registerService( MosaicHome.class, new MosaicHomeImpl(), null );

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
        this.bundleContext = null;
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
            Bundle systemBundle = this.bundleContext.getBundle( 0 );
            FrameworkWiring frameworkWiring = systemBundle.adapt( FrameworkWiring.class );
            frameworkWiring.resolveBundles( null );
            Bundle[] bundles = systemBundle.getBundleContext().getBundles();
            if( bundles != null ) {
                for( Bundle b : bundles ) {
                    if( b.getState() == Bundle.RESOLVED ) {
                        try {
                            b.start();
                        } catch( BundleException e ) {
                            LOG.warn( "Could not start bundle '{}': {}", BundleUtils.toString( b ), e.getMessage(), e );
                        }
                    }
                }
            }
/*
                try {
                    bundle.start();
                } catch( BundleException e ) {
                    LOG.warn( "Could not start bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                }
*/

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

    private static class MosaicHomeImpl implements MosaicHome {

        private final Path home;

        private final Path boot;

        private final Path deploy;

        private final Path etc;

        private final Path server;

        private final Path work;

        public MosaicHomeImpl() {
            this.home = Paths.get( System.getProperty( "mosaic.home" ) );
            this.boot = Paths.get( System.getProperty( "mosaic.home.boot" ) );
            this.deploy = Paths.get( System.getProperty( "mosaic.home.deploy" ) );
            this.etc = Paths.get( System.getProperty( "mosaic.home.etc" ) );
            this.server = Paths.get( System.getProperty( "mosaic.home.server" ) );
            this.work = Paths.get( System.getProperty( "mosaic.home.work" ) );
        }

        @Override
        public Path getHome() {
            return this.home;
        }

        @Override
        public Path getBoot() {
            return this.boot;
        }

        @Override
        public Path getDeploy() {
            return this.deploy;
        }

        @Override
        public Path getEtc() {
            return this.etc;
        }

        @Override
        public Path getServer() {
            return this.server;
        }

        @Override
        public Path getWork() {
            return this.work;
        }
    }
}
