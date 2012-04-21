package org.mosaic.server.boot.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.BundleState;
import org.mosaic.osgi.BundleStatus;
import org.mosaic.osgi.BundleStatusHelper;
import org.mosaic.osgi.util.BundleUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.osgi.framework.*;

/**
 * @author arik
 */
public class BundleBootstrapper implements SynchronousBundleListener, BundleStatusHelper {

    public static final Logger INSTALL_LOG = LoggerFactory.getLogger( LoggerFactory.getBundleLogger( BundleBootstrapper.class ).getName() + ".install" );

    public static final Logger RESOLVE_LOG = LoggerFactory.getLogger( LoggerFactory.getBundleLogger( BundleBootstrapper.class ).getName() + ".resolve" );

    public static final Logger ACTIVATION_LOG = LoggerFactory.getLogger( LoggerFactory.getBundleLogger( BundleBootstrapper.class ).getName() + ".activation" );

    private static final Logger LOG = LoggerFactory.getLogger( BundleBootstrapper.class );

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
        LOG.debug( "Opening bundle bootstrapper" );

        this.helperReg = this.bundleContext.registerService( BundleStatusHelper.class, this, null );
        this.bundleContext.addBundleListener( this );

        for( Bundle bundle : BundleUtils.findBundlesInStates( this.bundleContext, Bundle.ACTIVE ) ) {
            if( bundle.getBundleId() != this.bundleContext.getBundle().getBundleId() && shouldTrackBundle( bundle ) ) {
                trackBundle( bundle );
            }
        }

        LOG.info( "Opened bundle bootstrapper" );
    }

    public void close() {
        LOG.debug( "Stopping bundle bootstrapper" );

        for( BundleTracker tracker : this.trackers.values() ) {
            tracker.untrack();
        }
        if( this.helperReg != null ) {
            try {
                this.helperReg.unregister();
            } catch( IllegalArgumentException ignore ) {
            }
        }
        this.bundleContext.removeBundleListener( this );

        LOG.info( "Stopped bundle bootstrapper" );
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        switch( event.getType() ) {
            case BundleEvent.INSTALLED:
                INSTALL_LOG.info( "Installed bundle '{}'", BundleUtils.toString( bundle ), BundleUtils.toString( event.getOrigin() ) );
                break;

            case BundleEvent.RESOLVED:
                RESOLVE_LOG.info( "Resolved bundle '{}'", bundle );
                break;

            case BundleEvent.STARTING:
                ACTIVATION_LOG.debug( "Starting bundle '{}'", bundle );
                break;

            case BundleEvent.STARTED:
                ACTIVATION_LOG.info( "Started bundle '{}'", bundle );
                if( shouldTrackBundle( bundle ) ) {
                    trackBundle( bundle );
                }
                break;

            case BundleEvent.UPDATED:
                ACTIVATION_LOG.info( "Updated bundle '{}'", bundle );
                break;

            case BundleEvent.STOPPING:
                ACTIVATION_LOG.debug( "Stopping bundle '{}'", bundle );
                BundleTracker tracker = this.trackers.remove( bundle.getBundleId() );
                if( tracker != null ) {
                    try {
                        tracker.untrack();
                    } catch( Exception e ) {
                        LOG.error( "An error occurred while removing bundle '{}' from the list of tracked bundles: {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                    }
                }
                break;

            case BundleEvent.STOPPED:
                ACTIVATION_LOG.info( "Stopped bundle '{}'", bundle );
                break;

            case BundleEvent.UNRESOLVED:
                RESOLVE_LOG.info( "Unresolved bundle '{}'", bundle );
                break;

            case BundleEvent.UNINSTALLED:
                INSTALL_LOG.info( "Uninstalled bundle '{}'", bundle );
                break;
        }
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

    private boolean shouldTrackBundle( Bundle bundle ) {
        if( bundle.getEntryPaths( "/META-INF/spring/" ) == null ) {

            // has no spring files therefor there is no use for it to be tracked
            return false;

        } else if( bundle.getHeaders().get( Constants.BUNDLE_ACTIVATOR ) != null ) {

            // mosaic bundles must not have activators
            LOG.warn( "Bundle '{}' has Spring bean files in '/META-INF/spring', but also has a 'Bundle-Activator' header; Mosaic bundles must have no activator, and therefor will be ignored and treated as a standard bundle.", BundleUtils.toString( bundle ) );
            return false;

        } else {

            // bundle has spring files, and has no activator - approved
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
