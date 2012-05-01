package org.mosaic.server.boot.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.Home;
import org.mosaic.server.boot.impl.logging.LogWeaver;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.Requirement;
import org.mosaic.server.boot.impl.publish.spring.OsgiSpringNamespacePlugin;
import org.mosaic.server.osgi.BundleState;
import org.mosaic.server.osgi.BundleStatus;
import org.mosaic.server.osgi.BundleStatusHelper;
import org.mosaic.server.osgi.util.BundleUtils;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.*;

/**
 * @author arik
 */
public class BundleBootstrapper implements BundleActivator, SynchronousBundleListener, BundleStatusHelper {

    private static final Logger LOG = LoggerFactory.getLogger( BundleBootstrapper.class );

    private BundleContext bundleContext;

    private OsgiSpringNamespacePlugin springNamespacePlugin;

    private LogWeaver logWeaver;

    private Map<Long, BundleTracker> trackers = new ConcurrentHashMap<>( 100 );

    private ServiceRegistration<BundleStatusHelper> helperReg;

    @Override
    public void start( BundleContext context ) throws Exception {
        this.bundleContext = context;
        this.bundleContext.registerService( Home.class, new HomeService(), null );

        this.springNamespacePlugin = new OsgiSpringNamespacePlugin( this.bundleContext );
        this.springNamespacePlugin.open();

        this.logWeaver = new LogWeaver();
        this.logWeaver.open( this.bundleContext );

        this.helperReg = this.bundleContext.registerService( BundleStatusHelper.class, this, null );
        this.bundleContext.addBundleListener( this );

        for( Bundle bundle : BundleUtils.findBundlesInStates( this.bundleContext, Bundle.ACTIVE ) ) {
            if( bundle.getBundleId() != this.bundleContext.getBundle().getBundleId() && shouldTrackBundle( bundle ) ) {
                trackBundle( bundle );
            }
        }
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
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

        this.logWeaver.close();
        this.logWeaver = null;

        this.springNamespacePlugin.close();
        this.springNamespacePlugin = null;

        this.bundleContext = null;
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

    @Override
    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        switch( event.getType() ) {
            case BundleEvent.STARTED:
                if( shouldTrackBundle( bundle ) ) {
                    trackBundle( bundle );
                }
                break;

            case BundleEvent.STOPPING:
                BundleTracker tracker = this.trackers.remove( bundle.getBundleId() );
                if( tracker != null ) {
                    try {
                        tracker.untrack();
                    } catch( Exception e ) {
                        LOG.error( "An error occurred while removing bundle '{}' from the list of tracked bundles: {}", BundleUtils.toString( bundle ), e.getMessage(), e );
                    }
                }
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
