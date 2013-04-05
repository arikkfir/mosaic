package org.mosaic.launcher.osgi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.parseInt;
import static java.nio.file.Files.*;
import static org.apache.commons.lang3.text.StrSubstitutor.replaceSystemProperties;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;

/**
 * @author arik
 */
public class BundlesInstaller implements FileVisitor<Path>
{
    private static final Logger LOG = LoggerFactory.getLogger( BundlesInstaller.class );

    private static final String MOSAIC_START_LEVEL = "Mosaic-StartLevel";

    public static void watchDirectory( Path directory )
    {
        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            Dictionary<String, Object> dict = new Hashtable<>();
            dict.put( "root", directory.toString() );
            dict.put( "modificationsOnly", "false" );
            felix.getBundleContext().registerService( FileVisitor.class, new BundlesInstaller( directory ), dict );
        }
    }

    private static String toString( Bundle bundle )
    {
        if( bundle == null )
        {
            return "";
        }
        else
        {
            return bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
        }
    }

    private static class BundleLocationCache
    {
        private final Path path;

        private long readTime;

        private Set<Path> targets;

        private BundleLocationCache( Path path, long readTime )
        {
            this.path = path;
            this.readTime = readTime;
        }
    }

    private final Map<Path, BundleLocationCache> fileLocationCache = new HashMap<>();

    private final Map<Path, Long> fileFailures = new HashMap<>();

    private final Path root;

    private Collection<Bundle> bundlesToStart;

    private boolean bundlesWereUninstalled;

    public BundlesInstaller( Path root )
    {
        this.root = root;
    }

    @Override
    public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
    {
        // is this the scan initialization?
        if( dir != null )
        {
            return FileVisitResult.CONTINUE;
        }
        else
        {
            this.bundlesToStart = new LinkedList<>();
            this.bundlesWereUninstalled = false;
            return null;
        }
    }

    @Override
    public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
    {
        String lowerCasedFileName = file.getFileName().toString().toLowerCase();
        if( lowerCasedFileName.endsWith( ".jar" ) )
        {
            handleJarFile( file );
        }
        else if( lowerCasedFileName.endsWith( ".jars" ) || lowerCasedFileName.endsWith( ".maven" ) )
        {
            handleJarsFile( file );
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException
    {
        if( exc != null )
        {
            LOG.warn( "Error while visiting file at '{}': {}", file, exc.getMessage(), exc );
        }
        else
        {
            LOG.warn( "Unknown error while visiting file at '{}'", file );
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException
    {
        if( dir != null )
        {
            return FileVisitResult.CONTINUE;
        }
        else
        {
            // uninstall bundles installed by us whose files were removed
            Felix felix = FelixResolver.felix;
            if( felix != null )
            {
                for( Bundle bundle : felix.getBundleContext().getBundles() )
                {
                    String location = bundle.getLocation();
                    if( location != null && location.startsWith( this.root.toString() + "/" ) )
                    {
                        Path path = Paths.get( location );
                        if( notExists( path ) )
                        {
                            LOG.debug( "Deleted JAR file detected at: {} (uninstalling bundle '{}')", path, toString( bundle ) );
                            if( uninstallBundle( bundle ) )
                            {
                                this.bundlesWereUninstalled = true;
                            }
                        }
                    }
                }

                // uninstall bundles that were installed because of 'jars' files which no longer exist
                Set<BundleLocationCache> jarCollectionsToRemove = null;
                for( Iterator<Map.Entry<Path, BundleLocationCache>> iterator = this.fileLocationCache.entrySet().iterator(); iterator.hasNext(); )
                {
                    Map.Entry<Path, BundleLocationCache> entry = iterator.next();
                    if( notExists( entry.getKey() ) )
                    {
                        if( jarCollectionsToRemove == null )
                        {
                            jarCollectionsToRemove = new HashSet<>();
                        }
                        jarCollectionsToRemove.add( entry.getValue() );
                        iterator.remove();
                    }
                }
                if( jarCollectionsToRemove != null )
                {
                    for( BundleLocationCache cache : jarCollectionsToRemove )
                    {
                        for( Path jar : cache.targets )
                        {
                            boolean referenced = false;
                            for( BundleLocationCache validCache : this.fileLocationCache.values() )
                            {
                                if( validCache.targets.contains( jar ) )
                                {
                                    // jar is still referenced from any other jar collection - do not uninstall it
                                    referenced = true;
                                    break;
                                }
                            }
                            if( !referenced && !jar.startsWith( this.root ) )
                            {
                                Bundle bundle = felix.getBundleContext().getBundle( jar.toString() );
                                if( bundle != null )
                                {
                                    LOG.debug( "Bundle '{}' no longer referenced by {} or any other jars file - uninstalling it", toString( bundle ), cache.path );
                                    if( uninstallBundle( bundle ) )
                                    {
                                        this.bundlesWereUninstalled = true;
                                    }
                                }
                            }
                        }
                    }
                }

                // if any bundle was uninstalled or we have any bundle to start, we should perform a package refresh now
                if( this.bundlesWereUninstalled || !this.bundlesToStart.isEmpty() )
                {
                    // refresh packages so bundles are re-wired
                    final AtomicBoolean refreshed = new AtomicBoolean( false );
                    felix.getBundleContext().getBundle( 0 ).adapt( FrameworkWiring.class ).refreshBundles( null, new FrameworkListener()
                    {
                        @Override
                        public void frameworkEvent( FrameworkEvent event )
                        {
                            if( !bundlesToStart.isEmpty() )
                            {
                                LOG.debug( "Refreshing OSGi packages before starting {} new/updated bundles", bundlesToStart.size() );
                            }
                            else if( bundlesWereUninstalled )
                            {
                                LOG.debug( "Refreshed OSGi packages (some bundles were uninstalled)" );
                            }
                            refreshed.set( true );
                        }
                    } );

                    // wait until refresh operation is completed
                    while( true )
                    {
                        if( refreshed.get() )
                        {
                            // start any bundles that were installed or updated
                            for( Bundle bundle : this.bundlesToStart )
                            {
                                startBundle( bundle );
                            }
                            break;
                        }
                        else
                        {
                            try
                            {
                                Thread.sleep( 100 );
                            }
                            catch( InterruptedException e )
                            {
                                break;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    private void handleJarFile( Path sourceFile ) throws IOException
    {
        // check whether we need to install a new bundle or update an existing bundle
        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            Bundle bundle = felix.getBundleContext().getBundle( sourceFile.toString() );
            if( bundle == null )
            {
                // we never (successfully) installed this file
                Long lastFailure = this.fileFailures.get( sourceFile );

                // if we never failed trying to install this file, or if the file was updated AFTER the last failure, install it
                if( lastFailure == null || lastFailure < Files.getLastModifiedTime( sourceFile ).toMillis() )
                {
                    if( ( bundle = installBundle( sourceFile ) ) != null )
                    {
                        // installed successfully - remember it for later so we will try to start it
                        this.bundlesToStart.add( bundle );
                    }
                }
            }
            else
            {
                long fileLastModifiedTime = getLastModifiedTime( sourceFile ).toMillis();
                if( fileLastModifiedTime + 2000 < System.currentTimeMillis() && bundle.getLastModified() < fileLastModifiedTime )
                {
                    // this file was installed previously - update it IF we have not failed the last
                    // time we tried to update it OR if the file has been updated AFTER the last failure
                    Long lastFailure = this.fileFailures.get( sourceFile );
                    if( lastFailure == null || lastFailure < fileLastModifiedTime )
                    {
                        // log the change, stop the bundle, update it, and remember it for start later on
                        LOG.debug( "Updated JAR file detected at '{}' (updating bundle '{}')", sourceFile, toString( bundle ) );
                        if( stopAndUpdateBundle( bundle, sourceFile ) )
                        {
                            this.bundlesToStart.add( bundle );
                        }
                    }
                }
            }
        }
    }

    private void handleJarsFile( Path file ) throws IOException
    {
        // obtain list of installed bundles specified in this file from previous runs
        Set<Path> oldTargets = null;
        BundleLocationCache cache = this.fileLocationCache.get( file );
        if( cache != null )
        {
            oldTargets = cache.targets;
        }

        // obtain an up-to-date list of bundles this file specifies to install
        Set<Path> updatedTargets = readJarsFileTargets( file );

        // uninstall any bundles present in the file on previous runs, but were removed from the file now
        if( oldTargets != null )
        {
            for( Path oldTarget : oldTargets )
            {
                if( !updatedTargets.contains( oldTarget ) )
                {
                    Felix felix = FelixResolver.felix;
                    if( felix != null )
                    {
                        Bundle oldBundle = felix.getBundleContext().getBundle( oldTarget.toString() );
                        if( oldBundle != null )
                        {
                            LOG.debug( "JAR file '{}' has been removed from '{}' (uninstalling bundle '{}')", oldTarget, file, toString( oldBundle ) );
                            if( uninstallBundle( oldBundle ) )
                            {
                                this.bundlesWereUninstalled = true;
                            }
                        }
                    }
                }
            }
        }

        // install new bundles
        for( Path target : updatedTargets )
        {
            if( exists( target ) )
            {
                handleJarFile( target );
            }
        }
    }

    private Set<Path> readJarsFileTargets( Path file ) throws IOException
    {
        long fileModificationTime = getLastModifiedTime( file ).toMillis();

        // check if we have the list of targets for this file already cached - and if that cache is not stale
        BundleLocationCache cache = this.fileLocationCache.get( file );
        if( cache != null )
        {
            if( cache.readTime >= fileModificationTime )
            {
                // cache is not stale - return the cache
                return cache.targets;
            }
            else
            {
                // cache is stale - reset the cache entry before proceeding to read the target locations
                cache.targets = null;
                cache.readTime = fileModificationTime;
            }
        }
        else
        {
            // no cache - create a new cache entry and proceed to read the target locations
            cache = new BundleLocationCache( file, fileModificationTime );
            this.fileLocationCache.put( file, cache );
        }

        // is this a maven file?
        boolean maven = file.getFileName().toString().toLowerCase().endsWith( ".maven" );

        // read the locations of target files for this file
        try
        {
            Set<Path> files = new LinkedHashSet<>();
            for( String line : readAllLines( file, Charset.forName( "UTF-8" ) ) )
            {
                String trimmedLine = line.trim();
                if( !trimmedLine.isEmpty() && !trimmedLine.startsWith( "#" ) )
                {
                    Path target;
                    if( maven )
                    {
                        String[] tokens = line.split( ":" );
                        if( tokens.length != 3 )
                        {
                            LOG.warn( "Illegal Maven artifact ID found in file '{}': {}", file, line );
                            continue;
                        }

                        target = Paths.get(
                                System.getProperty( "user.home" ),
                                ".m2",
                                "repository",
                                tokens[ 0 ].replace( '.', '/' ),               // group ID
                                tokens[ 1 ],                                   // artifact ID
                                tokens[ 2 ],                                   // version
                                tokens[ 1 ] + "-" + tokens[ 2 ] + ".jar"       // ${artifactId}-${version}.jar
                        );
                    }
                    else
                    {
                        String location = replaceSystemProperties( trimmedLine );
                        target = Paths.get( location );
                        if( !target.isAbsolute() )
                        {
                            target = file.resolveSibling( location ).normalize().toAbsolutePath();
                        }
                    }
                    files.add( target );
                }
            }
            cache.targets = files;
        }
        catch( Exception e )
        {
            LOG.warn( "Could not read target JARs from file '{}': {}", file, e.getMessage(), e );
            cache.targets = Collections.emptySet();
        }
        return cache.targets;
    }

    private Bundle installBundle( Path file )
    {
        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            try
            {
                Bundle bundle = felix.getBundleContext().installBundle( file.toString(), newInputStream( file ) );
                updateBundleStartLevel( bundle );
                this.fileFailures.remove( file );
                return bundle;
            }
            catch( Exception e )
            {
                LOG.error( "Could not install bundle from '{}': {}", file, e.getMessage(), e );
                this.fileFailures.put( file, System.currentTimeMillis() );
            }
        }
        return null;
    }

    private boolean stopAndUpdateBundle( Bundle bundle, Path file )
    {
        if( "Mosaic".equalsIgnoreCase( bundle.getHeaders().get( BUNDLE_VENDOR ) ) )
        {
            String bsn = bundle.getSymbolicName();
            if( "api".equalsIgnoreCase( bsn ) || "lifecycle".equalsIgnoreCase( bsn ) )
            {
                LOG.warn( "Updating of Mosaic API bundle is not supported - please restart the Mosaic server instead." );
                return false;
            }
        }

        try
        {
            bundle.stop();
            bundle.update( newInputStream( file ) );
            updateBundleStartLevel( bundle );
            this.fileFailures.remove( file );
            return true;
        }
        catch( Exception e )
        {
            LOG.error( "Could not update bundle '{}' from '{}': {}", toString( bundle ), file, e.getMessage(), e );
            this.fileFailures.put( file, System.currentTimeMillis() );
            return false;
        }
    }

    private void startBundle( Bundle bundle )
    {
        try
        {
            bundle.start();
        }
        catch( BundleException e )
        {
            LOG.error( "Could not START bundle '{}': {}", toString( bundle ), e.getMessage(), e );
            try
            {
                bundle.stop();
            }
            catch( BundleException e1 )
            {
                LOG.error( "Could not STOP bundle '{}' due to failure to START: {}", toString( bundle ), e1.getMessage(), e1 );
            }
        }
    }

    private boolean uninstallBundle( Bundle bundle )
    {
        if( "Mosaic".equalsIgnoreCase( bundle.getHeaders().get( BUNDLE_VENDOR ) ) )
        {
            String bsn = bundle.getSymbolicName();
            if( "api".equalsIgnoreCase( bsn ) || "lifecycle".equalsIgnoreCase( bsn ) )
            {
                LOG.warn( "Updating of Mosaic API bundle is not supported - please restart the Mosaic server instead." );
                return false;
            }
        }

        try
        {
            bundle.uninstall();
            return true;
        }
        catch( BundleException e )
        {
            LOG.error( "Could not uninstall bundle '{}' (its update location was removed): {}", toString( bundle ), e.getMessage(), e );
            return false;
        }
    }

    private void updateBundleStartLevel( Bundle bundle )
    {
        String startLevelValue = bundle.getHeaders().get( MOSAIC_START_LEVEL );
        if( startLevelValue != null )
        {
            try
            {
                bundle.adapt( BundleStartLevel.class ).setStartLevel( parseInt( startLevelValue ) );
            }
            catch( NumberFormatException e )
            {
                LOG.warn( "Bundle '{}' has an illegal '{}' header value: {}", toString( bundle ), MOSAIC_START_LEVEL, startLevelValue );
            }
        }
    }
}
