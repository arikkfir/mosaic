package org.mosaic.runner.watcher;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class WatchedJarFile implements WatchedResource {

    private static final int QUIET_PERIOD = 1000 * 2;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Felix felix;

    private final int startLevel;

    private final Path path;

    private final String location;

    public WatchedJarFile( Felix felix, int startLevel, Path path ) {
        this.felix = felix;
        this.startLevel = startLevel;
        this.path = path.normalize().toAbsolutePath();
        this.location = this.path.toUri().toString();
    }

    @Override
    public ScanResult check() throws IOException, BundleException {
        if( notExists( this.path ) ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            return uninstall();

        }

        BasicFileAttributes attr = Files.readAttributes( this.path, BasicFileAttributes.class );
        if( isDirectory( this.path ) || !attr.isRegularFile() ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            return uninstall();

        } else if( !isReadable( this.path ) ) {

            //
            // error - cannot read file
            //
            throw new AccessDeniedException( this.path.toString(), null, "not readable" );

        }

        //
        // if no bundle for our file exists, install it as a new bundle
        //
        Bundle bundle = this.felix.getBundleContext().getBundle( this.location );
        if( bundle == null ) {
            this.logger.debug( "Installing bundle from: {}", this.path );
            bundle = this.felix.getBundleContext().installBundle( this.location, Files.newInputStream( this.path, StandardOpenOption.READ ) );
            this.logger.info( "Installed bundle from: {}", this.path );

            BundleStartLevel bundleStartLevel = bundle.adapt( BundleStartLevel.class );
            if( bundleStartLevel != null ) {
                bundleStartLevel.setStartLevel( this.startLevel );
            } else {
                this.logger.warn( "Could not set start-level {} for bundle '{}' - system might be unstable", this.startLevel, this.path );
            }
            return ScanResult.INSTALLED;
        }

        //
        // our file is currently deployed - check for updates
        //
        if( attr.lastModifiedTime().toMillis() <= bundle.getLastModified() ) {
            this.logger.trace( "File '{}' has not been modified", this.path );
            return ScanResult.UP_TO_DATE;
        }

        //
        // give a quiet period to the file to ensure it's not still being written to
        //
        if( System.currentTimeMillis() - attr.lastModifiedTime().toMillis() < QUIET_PERIOD ) {
            this.logger.trace( "File '{}' has been modified, but still in the quiet period", this.path );
            return ScanResult.UP_TO_DATE;
        }

        //
        // update bundle
        //
        this.logger.debug( "Updating bundle {} from: {}", bundle.getBundleId(), this.path );
        bundle.update( Files.newInputStream( this.path, StandardOpenOption.READ ) );
        this.logger.info( "Updated bundle from: {}", this.path );
        return ScanResult.UPDATED;
    }

    @Override
    public ScanResult uninstall() throws BundleException {
        Bundle bundle = this.felix.getBundleContext().getBundle( this.location );
        if( bundle != null ) {

            this.logger.debug( "Uninstalling bundle from: {}", this.path );
            bundle.uninstall();
            this.logger.info( "Uninstalled bundle from: {}", this.path );
            return ScanResult.UNINSTALLED;

        } else {

            this.logger.debug( "File '{}' no longer exists", this.path );
            return ScanResult.INVALID;

        }
    }
}
