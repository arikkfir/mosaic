package org.mosaic.modules.impl;

import com.google.common.collect.Sets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatterBuilder;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleState;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.resource.PathEvent;
import org.mosaic.util.resource.PathWatcher;
import org.mosaic.util.resource.PathWatcherContext;
import org.osgi.framework.*;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;
import static org.mosaic.modules.impl.Activator.getModuleManager;
import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
final class ServerLibWatcher implements PathWatcher
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerLibWatcher.class );

    private static final PropertyPlaceholderResolver PROPERTY_PLACEHOLDER_RESOLVER = new PropertyPlaceholderResolver();

    @Nonnull
    private final Map<Path, JarCollection> jarCollections = new ConcurrentHashMap<>();

    private boolean firstScan = true;

    @Override
    public synchronized void handle( @Nonnull PathWatcherContext context ) throws Exception
    {
        Path file = context.getFile();
        String lcFilename = file.toString().toLowerCase();
        switch( context.getEvent() )
        {
            case CREATED:
                if( lcFilename.endsWith( ".jar" ) )
                {
                    handleCreatedJar( context );
                }
                else if( lcFilename.endsWith( ".jars" ) )
                {
                    handleCreatedJarCollection( context );
                }
                break;

            case MODIFIED:
                if( lcFilename.endsWith( ".jar" ) )
                {
                    handleModifiedJar( context );
                }
                else if( lcFilename.endsWith( ".jars" ) )
                {
                    handleModifiedJarCollection( context );
                }
                break;

            case DELETED:
                if( lcFilename.endsWith( ".jar" ) )
                {
                    handleDeletedJar( context );
                }
                else if( lcFilename.endsWith( ".jars" ) )
                {
                    handleDeletedJarCollection( context );
                }
                break;

            case NOT_MODIFIED:
                if( lcFilename.endsWith( ".jars" ) )
                {
                    checkJarCollectionForUpdates( context );
                }
                break;

            case SCAN_FINISHED:
                Map<Long, String> startErrors = new HashMap<>();
                for( Bundle bundle : getInstalledBundles( context ) )
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
                        }
                        else
                        {
                            LOG.error( "Could not resolve bundle '{}-{}[{}]': {}", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e.getMessage() );
                        }
                    }
                }

                if( this.firstScan )
                {
                    String startupDuration = "unknown";

                    Long launchTime = Long.getLong( "mosaic.launch.start" );
                    if( launchTime != null )
                    {
                        startupDuration = new PeriodFormatterBuilder()
                                .appendMinutes()
                                .appendSuffix( " minute", " minutes" )
                                .appendSeparatorIfFieldsBefore( " and " )
                                .appendSecondsWithOptionalMillis()
                                .appendSuffix( " second", " seconds" )
                                .printZeroRarelyLast()
                                .toFormatter().print( new Duration( launchTime, System.currentTimeMillis() ).toPeriod() );
                    }

                    StringBuilder msg = new StringBuilder();

                    msg.append( "\n\n******************************************************************************\n\n" );
                    msg.append( "  Mosaic server STARTED! (in " ).append( startupDuration ).append( ")\n" );
                    for( Module module : getModuleManager().getModules() )
                    {
                        if( module.getState() != ModuleState.ACTIVE )
                        {
                            ModuleImpl moduleImpl = ( ModuleImpl ) module;
                            msg.append( "\n  Module " ).append( module ).append( " could not be activated:\n" );

                            String startError = startErrors.get( module.getId() );
                            if( startError != null )
                            {
                                msg.append( "    -> " ).append( startError ).append( "\n" );
                            }

                            for( Lifecycle child : moduleImpl.getInactivatables() )
                            {
                                msg.append( "    -> " ).append( child ).append( "\n" );
                            }
                        }
                    }
                    msg.append( "\n******************************************************************************\n\n" );

                    LOG.warn( msg.toString() );
                    this.firstScan = false;
                }
        }
    }

    @Nonnull
    private BundleContext getBundleContext()
    {
        Bundle modulesBundle = FrameworkUtil.getBundle( getClass() );
        if( modulesBundle == null )
        {
            throw new IllegalStateException( "could not find 'org.mosaic.modules' bundle" );
        }

        BundleContext bundleContext = modulesBundle.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "could not obtain bundle context for 'org.mosaic.modules' bundle" );
        }
        return bundleContext;
    }

    @Nonnull
    private String getLocationFromFile( @Nonnull Path file )
    {
        Path absolutePath = file.normalize().toAbsolutePath();
        return "file:" + absolutePath;
    }

    @Nonnull
    private List<Bundle> getInstalledBundles( @Nonnull PathWatcherContext context )
    {
        @SuppressWarnings("unchecked")
        List<Bundle> installed = context.getAttributes().get( "installed", List.class );
        if( installed == null )
        {
            installed = new LinkedList<>();
            context.getAttributes().put( "installed", installed );
        }
        return installed;
    }

    private void handleCreatedJar( @Nonnull PathWatcherContext context )
    {
        try
        {
            String location = getLocationFromFile( context.getFile() );
            getInstalledBundles( context ).add( getBundleContext().installBundle( location ) );
        }
        catch( Exception e )
        {
            LOG.error( "Error installing module from '{}': {}", context.getFile(), e.getMessage(), e );
        }
    }

    private void handleModifiedJar( @Nonnull PathWatcherContext context )
    {
        Bundle bundle = getBundleContext().getBundle( getLocationFromFile( context.getFile() ) );
        if( bundle != null )
        {
            // remember if we need to start the bundle after the update is complete
            boolean started = bundle.getState() == Bundle.ACTIVE;

            // create a list of bundles we will want to start after this process
            List<Bundle> bundlesToStart = new LinkedList<>();
            if( started )
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
                LOG.error( "Error stopping module at '{}' (stopping in order to update it): {}", context.getFile(), e.getMessage(), e );
                return;
            }

            // finally: update the bundle
            try
            {
                bundle.update();
            }
            catch( Exception e )
            {
                LOG.error( "Error updating module at '{}': {}", context.getFile(), e.getMessage(), e );
            }

            // refresh packages
            final AtomicBoolean refreshComplete = new AtomicBoolean( false );
            FrameworkWiring frameworkWiring = getBundleContext().getBundle( 0 ).adapt( FrameworkWiring.class );
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
                    Thread.sleep( 100 );
                }
                catch( InterruptedException e )
                {
                    LOG.error( "Module update process of '{}' was interrupted", context.getFile() );
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
                }
            }
        }
    }

    private void handleDeletedJar( @Nonnull PathWatcherContext context )
    {
        Bundle bundle = getBundleContext().getBundle( getLocationFromFile( context.getFile() ) );
        if( bundle != null )
        {
            try
            {
                bundle.uninstall();
            }
            catch( Exception e )
            {
                LOG.error( "Error uninstalling module from '{}': {}", context.getFile(), e.getMessage(), e );
            }
        }
    }

    private void handleCreatedJarCollection( @Nonnull PathWatcherContext context )
    {
        JarCollection collection = new JarCollection( context.getFile() );
        this.jarCollections.put( context.getFile(), collection );
        collection.refresh( context );
    }

    private void handleModifiedJarCollection( @Nonnull PathWatcherContext context )
    {
        JarCollection collection = this.jarCollections.get( context.getFile() );
        if( collection != null )
        {
            collection.refresh( context );
        }
    }

    private void handleDeletedJarCollection( @Nonnull PathWatcherContext context )
    {
        JarCollection collection = this.jarCollections.remove( context.getFile() );
        if( collection != null )
        {
            collection.remove( context );
        }
    }

    private void checkJarCollectionForUpdates( @Nonnull PathWatcherContext context )
    {
        JarCollection collection = this.jarCollections.get( context.getFile() );
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

        private void refresh( @Nonnull PathWatcherContext context )
        {
            List<String> lines;
            try
            {
                lines = Files.readAllLines( this.file, Charset.forName( "UTF-8" ) );
            }
            catch( Exception e )
            {
                LOG.warn( "Unable to refresh file collection at '{}': {}", this.file, e.getMessage(), e );
                return;
            }

            // save old file set to uninstall bundles that were in the old set but not in the updated set
            Set<Path> oldFileSet = new HashSet<>( this.files );

            // build new file-set
            Set<Path> newFileSet = new HashSet<>();
            for( String line : lines )
            {
                Path normalizedPath = Paths.get( PROPERTY_PLACEHOLDER_RESOLVER.resolve( line ) );
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
                handleDeletedJar( new PathWatcherContextWrapper( context, DELETED, path ) );
            }
        }

        private void checkForUpdates( @Nonnull PathWatcherContext context )
        {
            for( Path path : this.files )
            {
                Bundle bundle = getBundleContext().getBundle( getLocationFromFile( path ) );
                if( notExists( path ) || isDirectory( path ) || !isReadable( path ) )
                {
                    if( bundle != null )
                    {
                        // file was installed, but now has been removed, so uninstall its bundle
                        handleDeletedJar( new PathWatcherContextWrapper( context, DELETED, path ) );
                    }
                }
                else if( bundle == null )
                {
                    // file has not been installed
                    handleCreatedJar( new PathWatcherContextWrapper( context, CREATED, path ) );
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
                        handleModifiedJar( new PathWatcherContextWrapper( context, MODIFIED, path ) );
                    }
                }
            }
        }

        private void remove( @Nonnull PathWatcherContext context )
        {
            for( Path path : this.files )
            {
                handleDeletedJar( new PathWatcherContextWrapper( context, DELETED, path ) );
            }
        }
    }

    private class PathWatcherContextWrapper implements PathWatcherContext
    {
        @Nonnull
        private final PathWatcherContext target;

        @Nonnull
        private final PathEvent event;

        @Nonnull
        private final Path file;

        private PathWatcherContextWrapper( @Nonnull PathWatcherContext target,
                                           @Nonnull PathEvent event, @Nonnull Path file )
        {
            this.target = target;
            this.event = event;
            this.file = file;
        }

        @Nonnull
        @Override
        public DateTime getScanStart()
        {
            return this.target.getScanStart();
        }

        @Nonnull
        @Override
        public Path getFile()
        {
            return this.file;
        }

        @Nonnull
        @Override
        public PathEvent getEvent()
        {
            return this.event;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getAttributes()
        {
            return this.target.getAttributes();
        }
    }
}
