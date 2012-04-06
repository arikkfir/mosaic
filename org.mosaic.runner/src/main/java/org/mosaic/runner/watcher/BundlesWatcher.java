package org.mosaic.runner.watcher;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import org.mosaic.runner.StartException;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class BundlesWatcher implements Runnable {

    private static final int DEFAULT_SCAN_INTERVAL = 1000;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String name;

    private final long scanInterval;

    private final Collection<WatchedResourceProvider> watchedResourceProviders = new LinkedList<>();

    private final Path directory;

    private Thread thread;

    private boolean stop;

    public BundlesWatcher( BundleContext bundleContext, String name, Path directory ) throws StartException {
        this.name = name;
        this.scanInterval = Long.getLong( "bundleScanInterval", DEFAULT_SCAN_INTERVAL );
        this.directory = directory;
        this.watchedResourceProviders.add( new JarLinksWatchedResourceProvider( bundleContext, directory ) );
        this.watchedResourceProviders.add( new JarsWatchedResourceProvider( bundleContext, directory ) );
    }

    public void start() {
        this.logger.info( "Watching bundles directory at: {}", this.directory );
        this.stop = false;
        this.thread = new Thread( this, this.name );
        this.thread.setDaemon( true );
        this.thread.start();
    }

    public void scan() {
        for( WatchedResourceProvider watchedResourceProvider : this.watchedResourceProviders ) {
            for( WatchedResource watchedResource : watchedResourceProvider.getWatchedResources() ) {
                watchedResource.checkForUpdates();
            }
        }
    }

    @Override
    public void run() {
        while( !this.stop ) {

            // sleep for a while
            try {
                Thread.sleep( this.scanInterval );
            } catch( InterruptedException e ) {
                break;
            }

            // scan all watched resources
            scan();

        }
        this.logger.info( "Stopped watching bundles directory at: {}", this.directory );
    }

    public void stop() {
        this.stop = true;
        if( this.thread != null ) {
            try {
                this.thread.join( 1000 * 30 );
            } catch( InterruptedException e ) {
                this.logger.warn( "Timed-out while waiting for bundles watcher to stop watching directory: {}", this.directory );
            }
        }
    }
}
