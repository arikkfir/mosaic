package org.mosaic.runner.watcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class WatchedJarLinkFile implements WatchedResource {

    private static final long ERROR_QUIET_TIME = 1000 * 30;

    private static final long MIN_ERROR_COUNT_FOR_QUIET = 5;

    private static final int QUIET_PERIOD = 1000 * 2;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Felix felix;

    private final int startLevel;

    private final Path path;

    private long lastScanTime;

    private long lastKnownModificationTime;

    private int successiveErrorCount = 0;

    private WatchedJarFile watchedJarFile;

    public WatchedJarLinkFile( Felix felix, int startLevel, Path path ) {
        this.felix = felix;
        this.startLevel = startLevel;
        this.path = path.normalize().toAbsolutePath();
    }

    @Override
    public ScanResult check() {
        long time = System.currentTimeMillis();

        ScanResult result;
        if( this.successiveErrorCount < MIN_ERROR_COUNT_FOR_QUIET || time - this.lastScanTime >= ERROR_QUIET_TIME ) {

            try {
                result = doCheck();
                this.successiveErrorCount = 0;

            } catch( Exception e ) {
                this.logger.error( "Error processing file '{}': {}", new Object[] { this.path, e.getMessage(), e } );
                this.successiveErrorCount++;
                result = ScanResult.ERROR;

            } finally {
                this.lastScanTime = time;
            }

        } else {

            this.logger.trace( "Skipping scan of: {} (too many errors, will wait a while before trying again)", this.path );
            result = ScanResult.SKIPPED;

        }
        return result;

    }

    private ScanResult doCheck() throws IOException, BundleException {
        if( notExists( this.path ) ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            this.logger.trace( "Link '{}' no longer exists - removing", this.path );
            return uninstall();

        }

        BasicFileAttributes attr = Files.readAttributes( this.path, BasicFileAttributes.class );
        if( isDirectory( this.path ) || !attr.isRegularFile() ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            this.logger.trace( "Link '{}' does not point to a regular file - removing", this.path );
            return uninstall();

        } else if( !isReadable( this.path ) ) {

            //
            // error - cannot read file
            //
            throw new AccessDeniedException( this.path.toString(), null, "not readable" );

        }

        if( this.watchedJarFile == null || this.lastKnownModificationTime == 0 ) {

            //
            // this the first time we're reading the link contents
            //
            this.logger.trace( "Initializing link '{}'", this.path );
            initWatchedJarFile();
            this.lastKnownModificationTime = attr.lastModifiedTime().toMillis();

        } else if( this.lastKnownModificationTime < attr.lastModifiedTime().toMillis() ) {

            if( System.currentTimeMillis() >= ( attr.lastModifiedTime().toMillis() + QUIET_PERIOD ) ) {

                //
                // if the link was modified since we last read it, reload it
                //
                this.logger.trace( "Link '{}' was modified - reinitializing", this.path );
                initWatchedJarFile();
                this.lastKnownModificationTime = attr.lastModifiedTime().toMillis();

            }

        }

        //
        // update bundle
        //
        ScanResult result = this.watchedJarFile.check();
        switch( result ) {
            case INVALID:
            case UNINSTALLED:
                //purposely returning UP_TO_DATE here since we don't want the container to remove this link from the watched items list
                return ScanResult.UP_TO_DATE;
            default:
                return result;
        }
    }

    ScanResult uninstall() throws BundleException {
        if( this.watchedJarFile != null ) {
            return this.watchedJarFile.uninstall();
        } else {
            return ScanResult.INVALID;
        }
    }

    private void initWatchedJarFile() throws IOException, BundleException {
        if( this.watchedJarFile != null ) {
            this.watchedJarFile.uninstall();
        }

        String contents = new String( Files.readAllBytes( this.path ), Charset.forName( "UTF-8" ) ).trim();
        Path actualFile = this.path.getParent().resolve( contents ).normalize().toAbsolutePath();
        this.watchedJarFile = new WatchedJarFile( this.felix, this.startLevel, actualFile );
    }
}
