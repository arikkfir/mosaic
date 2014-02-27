package org.mosaic.launcher;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.*;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;

/**
 * @author arik
 */
final class BundleScanner implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger( BundleScanner.class );

    @Nonnull
    private static final Map<String, Integer> BOOT_BUNDLES;

    static
    {
        Map<String, Integer> bootBundles = new HashMap<>();
        bootBundles.put( "com.fasterxml.classmate", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-annotations", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-core", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-databind", 1 );
        bootBundles.put( "com.fasterxml.jackson.dataformat.jackson-dataformat-csv", 1 );
        bootBundles.put( "com.google.guava", 1 );
        bootBundles.put( "javax.el-api", 1 );
        bootBundles.put( "jcl.over.slf4j", 1 );
        bootBundles.put( "joda-time", 1 );
        bootBundles.put( "log4j.over.slf4j", 1 );
        bootBundles.put( "org.apache.commons.lang3", 1 );
        bootBundles.put( "org.apache.felix.configadmin", 1 );
        bootBundles.put( "org.apache.felix.eventadmin", 1 );
        bootBundles.put( "org.apache.felix.log", 1 );
        bootBundles.put( "org.glassfish.web.javax.el", 1 );
        BOOT_BUNDLES = bootBundles;
    }

    private static final int DEFAULT_START_LEVEL = 3;

    @Nonnull
    private final PathMatcher jarsPathMatcher = FileSystems.getDefault().getPathMatcher( "glob:**/*.{jar,jars}" );

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final Map<Path, Long> knownTimes = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<Path, JarCollection> jarCollections = new ConcurrentHashMap<>();

    private boolean running;

    private boolean lastRunSuccessful;

    private boolean tempLastRunSuccessful;

    BundleScanner( @Nonnull BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
    }

    public boolean isLastRunSuccessful()
    {
        return this.lastRunSuccessful;
    }

    @Override
    public void run()
    {
        synchronized( this )
        {
            if( this.running )
            {
                return;
            }
            this.running = true;
        }

        this.tempLastRunSuccessful = true;
        try
        {
            final Context context = new Context();

            walkFileTree( Mosaic.getLib(), EnumSet.of( FOLLOW_LINKS ), MAX_VALUE, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file,
                                                  @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    if( BundleScanner.this.jarsPathMatcher.matches( file ) )
                    {
                        BundleScanner.this.visitFile( file, context );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );

            visitDeletedFiles();
            scanCompleted( context );
        }
        catch( Throwable e )
        {
            LOG.error( "Error while scanning Mosaic server lib directory: {}", e.getMessage(), e );
            this.tempLastRunSuccessful = false;
        }
        finally
        {
            this.running = false;
            this.lastRunSuccessful = this.tempLastRunSuccessful;
        }
    }

    public void visitFile( @Nonnull Path path, @Nonnull Context context )
    {
        // get file modification time
        Long fileModificationTime = getFileModificationTime( path );
        if( fileModificationTime == null )
        {
            return;
        }

        // get last known mod-time and store this new updated mod-time in our map
        Long lastModificationTime = this.knownTimes.get( path );
        this.knownTimes.put( path, fileModificationTime );

        // if this is a new file we did not know about before - file-created event
        if( lastModificationTime == null )
        {
            try
            {
                pathCreated( path, context );
            }
            catch( Exception e )
            {
                scanError( path, e );
            }
        }
        else if( fileModificationTime > lastModificationTime )
        {
            try
            {
                pathModified( path, context );
            }
            catch( Exception e )
            {
                scanError( path, e );
            }
        }
        else
        {
            try
            {
                pathUnmodified( path, context );
            }
            catch( Exception e )
            {
                scanError( path, e );
            }
        }
    }

    void visitDeletedFiles()
    {
        Iterator<Path> iterator = this.knownTimes.keySet().iterator();
        while( iterator.hasNext() )
        {
            Path path = iterator.next();
            if( isDirectory( path ) )
            {
                // a previous file has become a directory - remove it
                iterator.remove();
                LOG.warn( "A watched file at '{}' has been replaced by a directory - this event is not supported and will not be propagated to the path watcher" );
            }
            else if( notExists( path ) )
            {
                // file has been deleted
                iterator.remove();
                try
                {
                    pathDeleted( path );
                }
                catch( Exception e )
                {
                    scanError( path, e );
                }
            }
        }
    }

    @Nullable
    private Long getFileModificationTime( Path file )
    {
        try
        {
            long fileModificationTimeInMillis = Files.getLastModifiedTime( file ).toMillis();

            // ignore files modified in the last second (probably still being modified)
            if( fileModificationTimeInMillis < System.currentTimeMillis() - 1000 )
            {
                return fileModificationTimeInMillis;
            }
        }
        catch( IOException e )
        {
            LOG.warn( "Could not obtain file modification time for '{}': {}", file, e.getMessage(), e );
        }
        return null;
    }

    private void pathCreated( @Nonnull Path path, @Nonnull Context context )
    {
        String lcFilename = path.toString().toLowerCase();
        if( lcFilename.endsWith( ".jar" ) )
        {
            handleCreatedJar( path, context );
        }
        else if( lcFilename.endsWith( ".jars" ) )
        {
            handleCreatedJarCollection( path, context );
        }
    }

    private void pathModified( @Nonnull Path path, @Nonnull Context context )
    {
        String lcFilename = path.toString().toLowerCase();
        if( lcFilename.endsWith( ".jar" ) )
        {
            handleModifiedJar( path, context );
        }
        else if( lcFilename.endsWith( ".jars" ) )
        {
            handleModifiedJarCollection( path, context );
        }
    }

    private void pathUnmodified( @Nonnull Path path, @Nonnull Context context )
    {
        String lcFilename = path.toString().toLowerCase();
        if( lcFilename.endsWith( ".jars" ) )
        {
            checkJarCollectionForUpdates( path, context );
        }
    }

    private void pathDeleted( @Nonnull Path path )
    {
        String lcFilename = path.toString().toLowerCase();
        if( lcFilename.endsWith( ".jar" ) )
        {
            handleDeletedJar( path );
        }
        else if( lcFilename.endsWith( ".jars" ) )
        {
            handleDeletedJarCollection( path );
        }
    }

    private void scanCompleted( @Nonnull Context context )
    {
        Map<Long, String> startErrors = new HashMap<>();
        for( Bundle bundle : context.getInstalledBundles() )
        {
            try
            {
                bundle.start();
            }
            catch( BundleException e )
            {
                startErrors.put( bundle.getBundleId(), e.getMessage() );
                if( e.getType() != BundleException.RESOLVE_ERROR )
                {
                    LOG.error( "Could not start bundle '{}-{}[{}]': {}", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e.getMessage(), e );
                    this.tempLastRunSuccessful = false;
                }
                else
                {
                    LOG.error( "Could not resolve bundle '{}-{}[{}]': {}", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e.getMessage() );
                    this.tempLastRunSuccessful = false;
                }
            }
        }
    }

    void scanError( @Nonnull Path path, @Nonnull Throwable e )
    {
        LOG.warn( "Error while handling '{}': {}", this, path, e.getMessage(), e );
        this.tempLastRunSuccessful = false;
    }

    @Nonnull
    private String getLocationFromFile( @Nonnull Path file )
    {
        Path absolutePath = file.normalize().toAbsolutePath();
        return "file:" + absolutePath;
    }

    private void handleCreatedJar( @Nonnull Path path, @Nonnull Context context )
    {
        try
        {
            String location = getLocationFromFile( path );

            Bundle bundle = this.bundleContext.getBundle( location );
            if( bundle == null )
            {
                bundle = this.bundleContext.installBundle( location );
                bundle.adapt( BundleStartLevel.class ).setStartLevel( getAppropriateStartLevel( bundle ) );
            }
            context.getInstalledBundles().add( bundle );
        }
        catch( Exception e )
        {
            LOG.error( "Error installing module from '{}': {}", path, e.getMessage(), e );
            this.tempLastRunSuccessful = false;
        }
    }

    private int getAppropriateStartLevel( Bundle bundle )
    {
        String explicitStartLevel = bundle.getHeaders().get( "Start-Level" );
        if( explicitStartLevel != null )
        {
            try
            {
                return parseInt( explicitStartLevel );
            }
            catch( NumberFormatException ignore )
            {
            }
        }

        if( BOOT_BUNDLES.containsKey( bundle.getSymbolicName() ) )
        {
            return BOOT_BUNDLES.get( bundle.getSymbolicName() );
        }

        return DEFAULT_START_LEVEL;
    }

    private void handleModifiedJar( @Nonnull Path path, @Nonnull Context context )
    {
        Bundle bundle = this.bundleContext.getBundle( getLocationFromFile( path ) );
        if( bundle == null )
        {
            // oh no! no such bundle.. must be a mistake :)
            // simply delegate to our handleCreatedJar method
            handleCreatedJar( path, context );
        }
        else
        {
            // create a list of bundles we will want to start after this process
            // if this bundle is started, add it to the list, as it will need to be [re]started too
            List<Bundle> bundlesToStart = new LinkedList<>();
            if( bundle.getState() == Bundle.ACTIVE )
            {
                bundlesToStart.add( bundle );
            }

            // bundles that depend on packages provided by this bundle will be stopped by the package refresh, so add
            // them to the list of bundles we want to start
            BundleWiring bundleWiring = bundle.adapt( BundleWiring.class );
            if( bundleWiring != null )
            {
                List<BundleWire> packageWires = bundleWiring.getProvidedWires( PackageNamespace.PACKAGE_NAMESPACE );
                if( packageWires != null )
                {
                    for( BundleWire packageWire : packageWires )
                    {
                        // TODO: if requirer is not started, we should not [re]start it after
                        bundlesToStart.add( packageWire.getRequirer().getBundle() );
                    }
                }
            }

            // first stop the bundle
            try
            {
                bundle.stop();
            }
            catch( Exception e )
            {
                LOG.error( "Error stopping module at '{}' (stopping in order to update it): {}", path, e.getMessage(), e );
                this.tempLastRunSuccessful = false;
                return;
            }

            // finally: update the bundle
            try
            {
                bundle.update();
            }
            catch( Exception e )
            {
                LOG.error( "Error updating module at '{}': {}", path, e.getMessage(), e );
                this.tempLastRunSuccessful = false;
                return;
            }

            // refresh packages
            final AtomicBoolean refreshComplete = new AtomicBoolean( false );
            FrameworkWiring frameworkWiring = this.bundleContext.getBundle( 0 ).adapt( FrameworkWiring.class );
            if( frameworkWiring != null )
            {
                frameworkWiring.refreshBundles( null, new FrameworkListener()
                {
                    @Override
                    public void frameworkEvent( FrameworkEvent event )
                    {
                        refreshComplete.set( true );
                    }
                } );
            }
            while( !refreshComplete.get() )
            {
                try
                {
                    Thread.sleep( 10 );
                }
                catch( InterruptedException e )
                {
                    LOG.error( "Bundle update process of '{}' was interrupted", path );
                    this.tempLastRunSuccessful = false;
                    return;
                }
            }

            // start bundles
            for( Bundle bundleToStart : bundlesToStart )
            {
                try
                {
                    bundleToStart.start();
                }
                catch( BundleException e )
                {
                    LOG.warn( "Error starting bundle '{}[{}]' (was stopped because of UPDATE to bundle '{}[{}]'): {}",
                              bundleToStart.getSymbolicName(), bundleToStart.getBundleId(),
                              bundle.getSymbolicName(), bundle.getBundleId(),
                              e.getMessage(), e );
                    this.tempLastRunSuccessful = false;
                }
            }
        }
    }

    private void handleDeletedJar( @Nonnull Path path )
    {
        Bundle bundle = this.bundleContext.getBundle( getLocationFromFile( path ) );
        if( bundle != null )
        {
            try
            {
                bundle.uninstall();
            }
            catch( Exception e )
            {
                LOG.error( "Error uninstalling module from '{}': {}", path, e.getMessage(), e );
                this.tempLastRunSuccessful = false;
            }
        }
    }

    private void handleCreatedJarCollection( @Nonnull Path path, @Nonnull Context context )
    {
        JarCollection collection = new JarCollection( path );
        this.jarCollections.put( path, collection );
        collection.refresh( context );
    }

    private void handleModifiedJarCollection( @Nonnull Path path, @Nonnull Context context )
    {
        JarCollection collection = this.jarCollections.get( path );
        if( collection != null )
        {
            collection.refresh( context );
        }
    }

    private void handleDeletedJarCollection( @Nonnull Path path )
    {
        JarCollection collection = this.jarCollections.remove( path );
        if( collection != null )
        {
            collection.remove();
        }
    }

    private void checkJarCollectionForUpdates( @Nonnull Path path, @Nonnull Context context )
    {
        JarCollection collection = this.jarCollections.get( path );
        if( collection != null )
        {
            collection.checkForUpdates( context );
        }
    }

    private class JarCollection
    {
        @Nonnull
        private final Path file;

        @Nonnull
        private Set<Path> files = Collections.emptySet();

        private JarCollection( @Nonnull Path file )
        {
            this.file = file;
        }

        private void refresh( @Nonnull Context context )
        {
            List<String> lines;
            try
            {
                lines = Files.readAllLines( this.file, Charset.forName( "UTF-8" ) );
            }
            catch( Exception e )
            {
                LOG.warn( "Unable to refresh file collection at '{}': {}", this.file, e.getMessage(), e );
                BundleScanner.this.tempLastRunSuccessful = false;
                return;
            }

            // save old file set to uninstall bundles that were in the old set but not in the updated set
            Set<Path> oldFileSet = new HashSet<>( this.files );

            // build new file-set
            Set<Path> newFileSet = new HashSet<>();
            for( String line : lines )
            {
                Path normalizedPath = Paths.get( line );    // TODO: can we resolve system properties here?
                if( !normalizedPath.isAbsolute() )
                {
                    normalizedPath = this.file.getParent().resolve( normalizedPath );
                }
                Path path = normalizedPath.normalize().toAbsolutePath().normalize();
                newFileSet.add( path );
            }

            // now update our set: install new files and update existing if needed
            this.files = newFileSet;
            checkForUpdates( context );

            // find out which files in the old fileset were removed from the updated set - and uninstall their bundles
            for( Path path : Sets.difference( oldFileSet, newFileSet ) )
            {
                handleDeletedJar( path );
            }
        }

        private void checkForUpdates( @Nonnull Context context )
        {
            for( Path path : this.files )
            {
                Bundle bundle = BundleScanner.this.bundleContext.getBundle( getLocationFromFile( path ) );
                if( notExists( path ) || isDirectory( path ) || !isReadable( path ) )
                {
                    if( bundle != null )
                    {
                        // file was installed, but now has been removed, so uninstall its bundle
                        handleDeletedJar( path );
                    }
                }
                else if( bundle == null )
                {
                    // file has not been installed
                    handleCreatedJar( path, context );
                }
                else
                {
                    // get file modification time
                    long fileModificationTime;
                    try
                    {
                        fileModificationTime = getLastModifiedTime( path ).toMillis();
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "Could not extract modification time for file '{}': {}", path, e.getMessage(), e );
                        continue;
                    }

                    // has the file been modified since the last time we inspected it?
                    if( fileModificationTime > bundle.getLastModified() )
                    {
                        handleModifiedJar( path, context );
                    }
                }
            }
        }

        private void remove()
        {
            for( Path path : this.files )
            {
                handleDeletedJar( path );
            }
        }
    }

    private class Context
    {
        @Nonnull
        private Optional<List<Bundle>> installedBundles = Optional.absent();

        @Nonnull
        List<Bundle> getInstalledBundles()
        {
            if( !this.installedBundles.isPresent() )
            {
                List<Bundle> installedBundles = new LinkedList<>();
                this.installedBundles = Optional.of( installedBundles );
            }
            return this.installedBundles.get();
        }
    }
}
