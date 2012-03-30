package org.mosaic.runner.watcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarFile;
import org.apache.felix.framework.Felix;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class WatchedJarLinkFile implements WatchedResource {

    private static final int QUIET_PERIOD = 1000 * 2;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Felix felix;

    private final int startLevel;

    private final Path path;

    private final Map<Path, ErrorSuppressingWatchedResource> watchedResources = new HashMap<>();

    private Path watchedResourcesRoot;

    private String watchedResourcesPattern;

    private long lastKnownModificationTime;

    public WatchedJarLinkFile( Felix felix, int startLevel, Path path ) {
        this.felix = felix;
        this.startLevel = startLevel;
        this.path = path.normalize().toAbsolutePath();
    }

    @Override
    public ScanResult check() throws BundleException, IOException {
        if( notExists( this.path ) ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            this.logger.debug( "Link '{}' no longer exists - removing", this.path );
            return uninstall();

        }

        BasicFileAttributes attr = Files.readAttributes( this.path, BasicFileAttributes.class );
        if( isDirectory( this.path ) || !attr.isRegularFile() ) {

            //
            // file was deleted - uninstall (if we installed before) and return
            //
            this.logger.debug( "Link '{}' does not point to a regular file - removing", this.path );
            return uninstall();

        } else if( !isReadable( this.path ) ) {

            //
            // error - cannot read file
            //
            throw new AccessDeniedException( this.path.toString(), null, "not readable" );

        }

        if( this.watchedResourcesRoot == null ) {

            //
            // this the first time we're reading the link contents
            //
            this.logger.debug( "Initializing link '{}'", this.path );
            initialize();
            this.lastKnownModificationTime = attr.lastModifiedTime().toMillis();

        } else if( this.lastKnownModificationTime < attr.lastModifiedTime().toMillis() ) {

            if( System.currentTimeMillis() >= ( attr.lastModifiedTime().toMillis() + QUIET_PERIOD ) ) {

                //
                // if the link was modified since we last read it, reload it
                //
                this.logger.debug( "Link '{}' was modified - reinitializing", this.path );
                initialize();
                this.lastKnownModificationTime = attr.lastModifiedTime().toMillis();

            }

        }

        //
        // update bundle(s)
        //
        // NOTE: we purposely return UP_TO_DATE here since we don't want the container to remove this link from the
        //       watched items list until the actual link file is removed (see above)
        //
        update();
        return ScanResult.UP_TO_DATE;
    }

    private void initialize() throws IOException {
        String contents = new String( Files.readAllBytes( this.path ), Charset.forName( "UTF-8" ) ).trim();
        if( !contents.contains( ":" ) ) {
            throw new IllegalArgumentException( "Illegal JAR link pattern '" + contents + "' - must be in the form of \"<root-dir>:<pattern>\" (without the pointy brackets)" );
        }

        int colonIndex = contents.indexOf( ':' );
        this.watchedResourcesRoot = this.path.getParent().resolve( contents.substring( 0, colonIndex ) ).normalize().toAbsolutePath();
        this.watchedResourcesPattern = contents.substring( colonIndex + 1 );
    }

    private void update() throws IOException, BundleException {
        Collection<Path> retainedMatches = new LinkedHashSet<>();
        for( Path match : Files.newDirectoryStream( this.watchedResourcesRoot, this.watchedResourcesPattern ) ) {
            match = match.normalize().toAbsolutePath();
            if( this.watchedResources.containsKey( match ) ) {

                //
                // this match is already being tracked - but still add it to the set of retained matches,
                // so it won't be removed in the next section
                //
                retainedMatches.add( match );

            } else if( isBundle( match ) ) {

                //
                // new matching bundle - install and add it
                //
                WatchedResource resource = new WatchedJarFile( this.felix, this.startLevel, match );
                this.watchedResources.put( match, new ErrorSuppressingWatchedResource( resource ) );
                retainedMatches.add( match );

            }
        }

        //
        // now that we've updated our list of matching bundles - make them validate themselves and install/uninstall
        // their corresponding bundles to/from felix.
        //
        Collection<ErrorSuppressingWatchedResource> removedResources = new LinkedHashSet<>();
        for( Path path : new HashSet<>( this.watchedResources.keySet() ) ) {
            ErrorSuppressingWatchedResource resource = this.watchedResources.get( path );
            if( !retainedMatches.contains( path ) ) {

                //
                // our link file no longer points to this resource - remove it
                //
                this.logger.debug( "Removing indirectly watched resource: {}", path );
                removedResources.add( watchedResources.remove( path ) );

            } else {

                //
                // validate and update this resource, but don't remove it, since this link still points to it
                //
                resource.check();

            }
        }

        //
        // now that we've validated and installed our list of matching bundles - remove all resources that no longer
        // matches our pattern, uninstall and remove them
        //
        for( ErrorSuppressingWatchedResource resource : removedResources ) {
            resource.uninstall();
        }
    }

    @Override
    public ScanResult uninstall() throws IOException, BundleException {
        if( this.watchedResources != null ) {
            for( ErrorSuppressingWatchedResource resource : this.watchedResources.values() ) {
                resource.uninstall();
            }
        }
        return ScanResult.INVALID;
    }

    private boolean isBundle( Path file ) throws IOException {
        JarFile jarFile = new JarFile( file.toFile() );
        String bsn = jarFile.getManifest().getMainAttributes().getValue( "Bundle-SymbolicName" );
        return bsn != null;
    }
}
