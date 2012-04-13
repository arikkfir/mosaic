package org.mosaic.runner.watcher;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class BundlesWatcher implements Runnable {

    private static final int DEFAULT_SCAN_INTERVAL = 1000;

    private final String name;

    private final long scanInterval;

    private final Collection<WatchedResourceProvider> watchedResourceProviders = new LinkedList<>();

    private boolean stop;

    public BundlesWatcher( BundleContext bundleContext, Path... directories ) {
        this( bundleContext, "BundlesWatcher", directories );
    }

    public BundlesWatcher( BundleContext bundleContext, String name, Path... directories ) {
        this.name = name;
        this.scanInterval = Long.getLong( "bundleScanInterval", DEFAULT_SCAN_INTERVAL );
        for( Path directory : directories ) {
            this.watchedResourceProviders.add( new JarLinksWatchedResourceProvider( bundleContext, directory ) );
            this.watchedResourceProviders.add( new JarsWatchedResourceProvider( bundleContext, directory ) );
        }
    }

    public void start() {
        this.stop = false;
        Thread thread = new Thread( this, this.name );
        thread.setDaemon( true );
        thread.start();
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
    }
}
