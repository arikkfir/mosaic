package org.mosaic.runner.watcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.felix.framework.Felix;
import org.mosaic.runner.exit.StartException;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;

/**
 * @author arik
 */
public class BundlesWatcher implements Runnable {

    private final Logger logger;

    private final Felix felix;

    private final int startLevel;

    private final long scanInterval;

    private final Path directory;

    private final PathMatcher jarBundlesMatcher;

    private final PathMatcher jarLinkBundlesMatcher;

    private final Map<Path, ErrorSuppressingWatchedResource> watchedResources = new HashMap<>();

    public BundlesWatcher( Logger logger,
                           Felix felix,
                           int startLevel,
                           long scanInterval,
                           Path directory )
            throws StartException {
        this.logger = logger;
        this.felix = felix;
        this.startLevel = startLevel;
        this.scanInterval = scanInterval;
        this.directory = directory;
        this.jarBundlesMatcher = this.directory.getFileSystem().getPathMatcher( "glob:*.jar" );
        this.jarLinkBundlesMatcher = this.directory.getFileSystem().getPathMatcher( "glob:*.jarlink" );
    }

    public Thread start( String threadName ) throws IOException, BundleException {
        //
        // perform an initial scan so initialize the server
        //
        scan();

        //
        // start the background thread to keep scanning
        //
        Thread thread = new Thread( this, threadName );
        thread.setDaemon( true );
        thread.start();
        return thread;
    }

    @Override
    public void run() {
        long scanInterval = this.scanInterval;
        int successiveErrorCount = 0;
        while( true ) {
            try {
                Thread.sleep( scanInterval );
            } catch( InterruptedException e ) {
                break;
            }

            try {
                scan();
                scanInterval = this.scanInterval;

            } catch( IOException e ) {
                logger.error( "An I/O error occurred while scanning directory '{}': {}", new Object[] {
                        this.directory,
                        e.getMessage(),
                        e
                } );
                successiveErrorCount++;
                if( successiveErrorCount == 3 ) {
                    scanInterval = 1000 * 30;
                    logger.warn( "{} successive errors encountered while scanning '{}' - increasing scan interval to {}", successiveErrorCount, scanInterval );
                }
            }
        }
    }

    private void scan() throws IOException {
        Files.walkFileTree( this.directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                Path absoluteFile = file.normalize().toAbsolutePath();
                if( !watchedResources.containsKey( absoluteFile ) ) {
                    if( jarBundlesMatcher.matches( absoluteFile.getFileName() ) ) {
                        addWatchedResource( absoluteFile, new WatchedJarFile( felix, startLevel, absoluteFile ) );
                    } else if( jarLinkBundlesMatcher.matches( absoluteFile.getFileName() ) ) {
                        addWatchedResource( absoluteFile, new WatchedJarLinkFile( felix, startLevel, absoluteFile ) );
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        } );
        for( Path path : new HashSet<>( this.watchedResources.keySet() ) ) {
            ErrorSuppressingWatchedResource resource = this.watchedResources.get( path );
            ScanResult result = resource.check();
            if( result == ScanResult.UNINSTALLED || result == ScanResult.INVALID ) {
                this.logger.debug( "Removing watched resource: {}", path );
                this.watchedResources.remove( path );
            }
        }
    }

    private void addWatchedResource( Path file, WatchedResource resource ) {
        logger.debug( "Adding watched resource: {}", file );
        this.watchedResources.put( file, new ErrorSuppressingWatchedResource( resource ) );
    }
}
