package org.mosaic.runner.deploy.watcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import org.mosaic.runner.util.BundleUtils;
import org.mosaic.runner.util.SystemPropertyUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.readAllBytes;

/**
 * @author arik
 */
public class JarLinksWatchedResourceProvider implements WatchedResourceProvider {

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final BundleContext bundleContext;

    private final Path directory;

    private final Map<Path, WatchedResource> watchedJarLinks = new HashMap<>();

    private final JarLinkResourceHandler jarLinkResourceHandler = new JarLinkResourceHandler();

    public JarLinksWatchedResourceProvider( BundleContext bundleContext, Path directory ) {
        this.bundleContext = bundleContext;
        this.directory = directory;
    }

    @Override
    public Collection<WatchedResource> getWatchedResources() {
        try {

            //
            // check if any new files have been added to our watched directory; note that we don't remove from our map
            // any resources might not exist anymore - that will be done automatically when the 'handleNoLongerExists'
            // method is called.
            //
            Files.walkFileTree( this.directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                    file = file.normalize().toAbsolutePath();
                    if( !watchedJarLinks.containsKey( file ) && file.getFileName().toString().toLowerCase().endsWith( ".jars" ) ) {
                        //
                        // new, un-tracked JAR file - add it to our list of watched files
                        //
                        watchedJarLinks.put( file, new WatchedResource( file, jarLinkResourceHandler ) );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );

            //
            // return our list of watched resources
            //
            return this.watchedJarLinks.values();

        } catch( IOException e ) {
            logger.error( "Could not scan directory '{}' for JAR link files: {}",
                          new Object[] { this.directory, e.getMessage(), e } );
            return Collections.emptyList();
        }
    }

    private class JarLinks {

        private final Map<Path, WatchedResource> watchedJars = new HashMap<>();

        private Long lastUpdateTime;

        private Path linksRootDirectory;

        private String linksPattern;

        private void init( Path resource ) throws IOException {
            this.lastUpdateTime = System.currentTimeMillis();

            String contents = new String( readAllBytes( resource ), Charset.forName( "UTF-8" ) ).trim();
            if( !contents.contains( ":" ) ) {
                throw new IllegalStateException( "JAR link file '" + resource + "' contains an illegal pattern ('" + contents + "') - must be in the form of \"<root-dir>:<pattern>\" (without the pointy brackets)" );
            }

            int colonIndex = contents.indexOf( ':' );
            String path = SystemPropertyUtils.resolvePlaceholders( contents.substring( 0, colonIndex ), false );
            String pattern = SystemPropertyUtils.resolvePlaceholders( contents.substring( colonIndex + 1 ), false );
            this.linksRootDirectory = resource.getParent().resolve( path ).normalize().toAbsolutePath();
            this.linksPattern = pattern;
        }

        private void checkForUpdates() throws IOException {
            //
            // search for files that match our pattern
            //
            Collection<Path> updatedMatches = new LinkedHashSet<>();
            for( Path match : Files.newDirectoryStream( this.linksRootDirectory, this.linksPattern ) ) {
                match = match.normalize().toAbsolutePath();
                if( this.watchedJars.containsKey( match ) ) {

                    //
                    // this match is already being tracked - but still add it to the set of retained matches,
                    // so it won't be removed in the next section
                    //
                    updatedMatches.add( match );

                } else if( BundleUtils.isBundle( match ) ) {

                    //
                    // new matching bundle - add it to our list of watched resources
                    //
                    this.watchedJars.put( match, new WatchedResource( match, new JarWatchedResourceHandler( bundleContext ) ) );
                    updatedMatches.add( match );

                }
            }

            //
            // we've updated our list of files matching our pattern with potentially new files; now remove from our
            // watch list any matches that no longer match our updated pattern, and update those still match
            // NOTE: we first install all new JARs and only THEN we remove those we don't reference anymore
            //       this makes sure that if package refreshes can find alternate implementations in the new JARs if any
            //
            Collection<WatchedResource> removed = new LinkedList<>();
            for( Path path : new HashSet<>( this.watchedJars.keySet() ) ) {
                if( updatedMatches.contains( path ) ) {

                    //
                    // this jar still matches our pattern - check if it's updated and if so install/update its bundle
                    //
                    this.watchedJars.get( path ).checkForUpdates();

                } else {

                    //
                    // this jar no longer matches our pattern - uninstall it and remove it from our watch list
                    //
                    removed.add( this.watchedJars.remove( path ) );

                }
            }
            for( WatchedResource watchedResource : removed ) {
                watchedResource.handleNoLongerExists( watchedResource.getResource() );
            }
        }

        private void clearResources() {
            for( WatchedResource watchedResource : this.watchedJars.values() ) {
                watchedResource.handleNoLongerExists( watchedResource.getResource() );
            }
            this.watchedJars.clear();
        }
    }

    private class JarLinkResourceHandler implements WatchedResourceHandler {

        private final Map<Path, JarLinks> links = new HashMap<>();

        @Override
        public void handleNoLongerExists( Path resource ) {
            //
            // remove this links file from the list of watched resources, since it does not exist anymore
            //
            watchedJarLinks.remove( resource );

            //
            // uninstall all the JAR files we currently reference
            //
            JarLinks jarLinks = this.links.get( resource );
            if( jarLinks != null ) {
                jarLinks.clearResources();
            }
        }

        @Override
        public void handleIllegalFile( Path resource ) {
            //
            // this links file *exists*, but is also *currently* invalid (points to a directory, no permissions to read,
            // etc); for now, uninstall all the JAR files it currently references, but not removing from the list of
            // watched resources since it might become valid again (e.g. someone adds the missing permissions to the file)
            //
            JarLinks jarLinks = this.links.get( resource );
            if( jarLinks != null ) {
                jarLinks.clearResources();
            }
        }

        @Override
        public Long getLastUpdateTime( Path resource ) {
            JarLinks jarLinks = this.links.get( resource );
            if( jarLinks != null ) {
                return jarLinks.lastUpdateTime;
            } else {
                return null;
            }
        }

        @Override
        public void handleUpdated( Path resource ) throws IOException {
            JarLinks jarLinks = this.links.get( resource );
            if( jarLinks == null ) {
                jarLinks = new JarLinks();
                this.links.put( resource, jarLinks );
            }
            jarLinks.init( resource );
            handleUpToDate( resource );
        }

        @Override
        public void handleUpToDate( Path resource ) throws IOException {
            JarLinks jarLinks = this.links.get( resource );
            if( jarLinks != null ) {
                jarLinks.checkForUpdates();
            }
        }
    }
}
