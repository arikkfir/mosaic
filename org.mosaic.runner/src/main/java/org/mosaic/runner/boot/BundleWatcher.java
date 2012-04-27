package org.mosaic.runner.boot;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author arik
 */
public class BundleWatcher implements SynchronousBundleListener, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger( BundleWatcher.class );

    private final BundleContext bundleContext;

    private final Set<String> watchedLocations;

    public BundleWatcher( BundleContext bundleContext, Collection<String> watchedLocations ) {
        this.bundleContext = bundleContext;
        this.watchedLocations = new CopyOnWriteArraySet<>( watchedLocations );
        this.bundleContext.addBundleListener( this );
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate( this, 10, 1, SECONDS );
    }

    @Override
    public void run() {
        LOG.trace( "Scanning watched bundles" );

        Set<String> locations = new LinkedHashSet<>( this.watchedLocations );
        for( String location : locations ) {
            Bundle bundle = this.bundleContext.getBundle( location );
            if( bundle != null ) {
                File file = getBundleLocationAsFile( location );
                if( file.exists() && file.isFile() && file.lastModified() > bundle.getLastModified() ) {
                    try {
                        bundle.update();
                    } catch( BundleException e ) {
                        LOG.warn( "Could not update bundle '{}' from '{}': {}", new Object[] {
                                BundleUtils.toString( bundle ),
                                location,
                                e.getMessage(),
                                e
                        } );
                    }
                }
            }
        }
    }

    @Override
    public void bundleChanged( BundleEvent event ) {
        if( event.getType() == BundleEvent.UNINSTALLED ) {
            String location = event.getBundle().getLocation();
            if( location != null ) {
                this.watchedLocations.remove( location );
            }
        }
    }

    private File getBundleLocationAsFile( String location ) {
        if( !location.startsWith( "file:" ) ) {
            return null;
        } else {
            try {
                return new File( URI.create( location ) );
            } catch( Exception e ) {
                return null;
            }
        }
    }
}
