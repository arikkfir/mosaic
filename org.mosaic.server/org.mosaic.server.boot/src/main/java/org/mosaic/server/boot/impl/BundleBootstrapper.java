package org.mosaic.server.boot.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.lifecycle.BundleState;
import org.mosaic.lifecycle.BundleStatus;
import org.mosaic.lifecycle.BundleStatusHelper;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * @author arik
 */
public class BundleBootstrapper implements SynchronousBundleListener, BundleStatusHelper {

    private static final Logger LOG = LoggerFactory.getBundleLogger( BundleBootstrapper.class );

    private final BundleContext bundleContext;

    private final OsgiSpringNamespacePlugin springNamespacePlugin;

    private final Map<Long, BundleTracker> trackers = new ConcurrentHashMap<>( 100 );

    private ServiceRegistration<BundleStatusHelper> helperReg;

    public BundleBootstrapper( BundleContext bundleContext, OsgiSpringNamespacePlugin springNamespacePlugin ) {
        this.bundleContext = bundleContext;
        this.springNamespacePlugin = springNamespacePlugin;
    }

    @Override
    public BundleStatus getBundleStatus( long bundleId ) {
        Bundle bundle = this.bundleContext.getBundle( bundleId );
        if( bundle == null ) {
            return null;
        } else {
            return new BundleStatusImpl( bundle );
        }
    }

    public void open() {
        LOG.debug( "Opened bundle bootstrapper" );

        for( Bundle bundle : findBundlesInStates( Bundle.ACTIVE ) ) {
            if( shouldTrackBundle( bundle ) ) {
                trackBundle( bundle );
            }
        }

        this.bundleContext.addBundleListener( this );
        this.helperReg = this.bundleContext.registerService( BundleStatusHelper.class, this, null );
    }

    public void close() {
        if( this.helperReg != null ) {
            try {
                this.helperReg.unregister();
            } catch( IllegalArgumentException ignore ) {
            }
        }
        for( BundleTracker tracker : this.trackers.values() ) {
            tracker.untrack();
        }
        this.bundleContext.removeBundleListener( this );
        LOG.debug( "Stopped bundle bootstrapper" );
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        switch( event.getType() ) {
            case BundleEvent.INSTALLED:
                LOG.debug( "Installed bundle '{}'", BundleUtils.toString( bundle ), BundleUtils.toString( event.getOrigin() ) );
                startResolvedBundles();
                break;

            case BundleEvent.RESOLVED:
                LOG.debug( "Resolved bundle '{}'", bundle );
                break;

            case BundleEvent.STARTING:
                LOG.debug( "Starting bundle '{}'", bundle );
                break;

            case BundleEvent.LAZY_ACTIVATION:
                LOG.debug( "Lazy-activating bundle '{}' (will be activated lazily when needed)", bundle );
                break;

            case BundleEvent.STARTED:
                if( shouldTrackBundle( bundle ) ) {
                    trackBundle( bundle );
                } else {
                    LOG.info( "Started bundle '{}'", bundle );
                }
                break;

            case BundleEvent.UPDATED:
                LOG.debug( "Updated bundle '{}'", bundle );
                break;

            case BundleEvent.STOPPING:
                LOG.debug( "Stopping bundle '{}'", bundle );
                break;

            case BundleEvent.STOPPED:
                LOG.info( "Stopped bundle '{}'", bundle );
                BundleTracker tracker = this.trackers.remove( bundle.getBundleId() );
                if( tracker != null ) {
                    try {
                        tracker.untrack();
                    } catch( Exception e ) {
                        LOG.error( "An error occurred while removing bundle '{}' from the list of tracked bundles: {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                    }
                }
                break;

            case BundleEvent.UNRESOLVED:
                LOG.debug( "Unresolved bundle '{}'", bundle );
                break;

            case BundleEvent.UNINSTALLED:
                LOG.info( "Uninstalled bundle '{}'", bundle );
                break;
        }
    }

    private void startResolvedBundles() {
        resolveBundles();
        for( Bundle bundle : findBundlesInStates( Bundle.RESOLVED ) ) {
            if( bundle.getBundleId() != this.bundleContext.getBundle().getBundleId() ) {
                try {
                    bundle.start();
                } catch( BundleException e ) {
                    LOG.warn( "Could not start bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                }
            }
        }
    }

    private void resolveBundles() {
        Bundle systemBundle = this.bundleContext.getBundle( 0 );
        FrameworkWiring frameworkWiring = systemBundle.adapt( FrameworkWiring.class );
        frameworkWiring.resolveBundles( null );
    }

    private void trackBundle( Bundle bundle ) {
        BundleTracker tracker = new BundleTracker( bundle, this.springNamespacePlugin );
        try {
            tracker.track();
            this.trackers.put( bundle.getBundleId(), tracker );
        } catch( Exception e ) {
            LOG.error( "Cannot track bundle '{}': {}", BundleUtils.toString( bundle ), e.getMessage(), e );
        }
    }

    private Collection<Bundle> findBundlesInStates( Integer... states ) {
        Collection<Integer> bundleStates = Arrays.asList( states );
        Collection<Bundle> resolvedBundles = new LinkedList<>();
        for( Bundle bundle : BundleUtils.getAllBundles( this.bundleContext ) ) {
            if( bundleStates.contains( bundle.getState() ) ) {
                resolvedBundles.add( bundle );
            }
        }
        return resolvedBundles;
    }

    private boolean shouldTrackBundle( Bundle bundle ) {
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

    private class BundleStatusImpl implements BundleStatus {

        private final BundleState state;

        private final Collection<String> unsatisfiedRequirements;

        public BundleStatusImpl( Bundle bundle ) {
            BundleTracker tracker = trackers.get( bundle.getBundleId() );
            if( tracker != null ) {
                if( tracker.isTracking() && tracker.isPublished() ) {
                    this.state = BundleState.PUBLISHED;
                } else {
                    this.state = BundleState.ACTIVE;
                }

                List<String> unsatisfiedRequirements = new LinkedList<>();
                for( Requirement requirement : tracker.getUnsatisfiedRequirements() ) {
                    unsatisfiedRequirements.add( requirement.toShortString() );
                }
                this.unsatisfiedRequirements = Collections.unmodifiableCollection( unsatisfiedRequirements );

            } else {
                this.state = BundleState.valueOfOsgiState( bundle.getState() );
                this.unsatisfiedRequirements = Collections.emptyList();
            }
        }

        @Override
        public BundleState getState() {
            return this.state;
        }

        @Override
        public Collection<String> getUnsatisfiedRequirements() {
            return this.unsatisfiedRequirements;
        }
    }
}
