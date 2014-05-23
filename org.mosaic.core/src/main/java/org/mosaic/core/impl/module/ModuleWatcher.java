package org.mosaic.core.impl.module;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.*;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;

/**
 * @author arik
 */
public class ModuleWatcher extends TransitionAdapter
{
    @Nonnull
    private final Logger logger;

    @Nonnull
    private final Path libPath;

    @Nullable
    private ScheduledExecutorService executorService;

    public ModuleWatcher( @Nonnull Logger logger, @Nonnull Path libPath )
    {
        this.logger = logger;
        this.libPath = libPath;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            initialize();
        }
        else if( target == ServerStatus.STOPPED )
        {
            shutdown();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            shutdown();
        }
    }

    private void initialize()
    {
        this.logger.debug( "Initializing module watcher on {}", this.libPath );

        ThreadFactory threadFactory = new ThreadFactory()
        {
            @Override
            public Thread newThread( @Nonnull Runnable r )
            {
                Thread thread = new Thread( r, "PathScanner" );
                thread.setDaemon( false );
                return thread;
            }
        };

        this.executorService = Executors.newSingleThreadScheduledExecutor( threadFactory );
        this.executorService.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                scan();
            }
        }, 0, 1, TimeUnit.SECONDS );
    }

    private void shutdown() throws InterruptedException
    {
        this.logger.debug( "Shutting down module watcher" );

        ScheduledExecutorService executorService = this.executorService;
        if( executorService != null )
        {
            executorService.shutdown();
            try
            {
                executorService.awaitTermination( 30, TimeUnit.SECONDS );
            }
            finally
            {
                this.executorService = null;
            }
        }
    }

    synchronized void scan()
    {
        final Context context = new Context();

        // prepare for a new scan
        context.start();

        // scan existing files
        try
        {
            final PathMatcher jarMatcher = this.libPath.getFileSystem().getPathMatcher( "glob:**/*.jar" );
            walkFileTree( this.libPath, EnumSet.of( FOLLOW_LINKS ), MAX_VALUE, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
                {
                    // only process *.jar files
                    if( jarMatcher.matches( dir ) )
                    {
                        if( exists( dir ) )
                        {
                            context.handleFile( dir );
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    else
                    {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    // only process *.jar files
                    if( jarMatcher.matches( file ) && exists( file ) )
                    {
                        context.handleFile( file );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        catch( Throwable e )
        {
            this.logger.error( "Modules scanning error: {}", e.getMessage(), e );
        }

        try
        {
            // finish & apply scan results
            context.finish();
        }
        catch( Throwable e )
        {
            this.logger.error( "Scan error", e );
        }
    }

    private class Context
    {
        @Nonnull
        private final BundleContext bundleContext;

        @Nullable
        private List<Path> newModules = null;

        @Nullable
        private List<Bundle> modifiedModules = null;

        @Nullable
        private List<Bundle> modulesToStart = null;

        public Context()
        {
            Bundle coreBundle = FrameworkUtil.getBundle( getClass() );
            if( coreBundle == null )
            {
                throw new IllegalStateException( "could not find core bundle context" );
            }

            BundleContext coreBundleContext = coreBundle.getBundleContext();
            if( coreBundleContext == null )
            {
                throw new IllegalStateException( "could not find core bundle context" );
            }

            this.bundleContext = coreBundleContext;
        }

        private void start()
        {
            this.newModules = null;
            this.modifiedModules = null;
        }

        private void handleFile( @Nonnull Path path )
        {
            Bundle bundle = this.bundleContext.getBundle( "file:" + path.toString() );
            if( bundle == null )
            {
                this.newModules = addToList( this.newModules, path );
            }
            else
            {
                try
                {
                    // get file modification time, so we can compare it with the bundle's modification time
                    // we'll ignore files modified in the last second (probably still being modified)
                    long fileModTime = Files.getLastModifiedTime( path ).toMillis();
                    if( fileModTime <= System.currentTimeMillis() - 1000 )
                    {
                        // only update the corresponding bundle if it is NOT up-to-date
                        if( bundle.getLastModified() < fileModTime )
                        {
                            this.modifiedModules = addToList( this.modifiedModules, bundle );
                        }
                    }
                }
                catch( Exception e )
                {
                    handleFileError( "Could not extract file modification time of '{}': {}", path, e.getMessage(), e );
                }
            }
        }

        private void handleFileError( @Nonnull String message, @Nullable Object... args )
        {
            ModuleWatcher.this.logger.error( message, args );
        }

        private void finish() throws InterruptedException
        {
            // uninstall modules that no longer exist
            removeOldModules();

            // install new modules
            installNewModules();

            // update modules
            updateModifiedModules();

            // start installed modules
            startModules();
        }

        private void refreshDependantsOf( @Nonnull Collection<Bundle> bundles ) throws InterruptedException
        {
            FrameworkWiring frameworkWiring = this.bundleContext.getBundle( 0 ).adapt( FrameworkWiring.class );
            for( Bundle bundle : frameworkWiring.getDependencyClosure( bundles ) )
            {
                if( bundle.getState() == Bundle.ACTIVE )
                {
                    try
                    {
                        bundle.stop();
                    }
                    catch( BundleException e )
                    {
                        handleFileError( "Could not stop module from '{}': {}", bundle.getLocation(), e.getMessage(), e );
                    }
                    this.modulesToStart = addToList( this.modulesToStart, bundle );
                }
            }

            final AtomicBoolean done = new AtomicBoolean( false );
            frameworkWiring.refreshBundles( bundles, new FrameworkListener()
            {
                @Override
                public void frameworkEvent( FrameworkEvent event )
                {
                    done.set( true );
                }
            } );
            while( !done.get() )
            {
                Thread.sleep( 100 );
            }
        }

        private void removeOldModules() throws InterruptedException
        {
            List<Bundle> removedBundles = null;
            for( Bundle bundle : this.bundleContext.getBundles() )
            {
                if( bundle.getBundleId() > 0 )
                {
                    String location = bundle.getLocation();
                    if( location.startsWith( "file:" ) )
                    {
                        location = location.substring( "file:".length() );
                    }

                    Path path = Paths.get( location );
                    if( notExists( path ) )
                    {
                        removedBundles = addToList( removedBundles, bundle );
                        try
                        {
                            bundle.uninstall();
                        }
                        catch( Exception e )
                        {
                            handleFileError( "Could not uninstall module from '{}': {}", path, e.getMessage(), e );
                        }
                    }
                }
            }
            if( removedBundles != null )
            {
                refreshDependantsOf( removedBundles );
            }
        }

        private void installNewModules()
        {
            List<Path> newModulePaths = this.newModules;
            if( newModulePaths != null )
            {
                for( Path path : newModulePaths )
                {
                    try
                    {
                        this.modulesToStart = addToList(
                                this.modulesToStart,
                                this.bundleContext.installBundle( "file:" + path.toString() )
                        );
                    }
                    catch( Exception e )
                    {
                        handleFileError( "Could not install module from '{}': {}", path, e.getMessage(), e );
                    }
                }
            }
        }

        private void updateModifiedModules() throws InterruptedException
        {
            List<Bundle> modifiedModules = this.modifiedModules;
            if( modifiedModules != null )
            {
                for( Bundle bundle : modifiedModules )
                {
                    if( bundle.getState() == Bundle.ACTIVE )
                    {
                        try
                        {
                            bundle.stop();
                        }
                        catch( BundleException e )
                        {
                            handleFileError( "Could not stop module from '{}': {}", bundle.getLocation(), e.getMessage(), e );
                        }
                        this.modulesToStart = addToList( this.modulesToStart, bundle );
                    }
                    try
                    {
                        bundle.update();
                    }
                    catch( Exception e )
                    {
                        handleFileError( "Could not update module from '{}': {}", bundle.getLocation(), e.getMessage(), e );
                    }
                }
                refreshDependantsOf( modifiedModules );
            }
        }

        private void startModules()
        {
            List<Bundle> modulesToStart = this.modulesToStart;
            if( modulesToStart != null )
            {
                for( Bundle bundle : modulesToStart )
                {
                    try
                    {
                        bundle.start();
                    }
                    catch( Exception e )
                    {
                        handleFileError( "Could not start module from '{}': {}", bundle.getLocation(), e.getMessage(), e );
                    }
                }
            }
        }

        @Nonnull
        private <T> List<T> addToList( @Nullable List<T> list, @Nonnull T item )
        {
            if( list == null )
            {
                list = new LinkedList<>();
            }
            list.add( item );
            return list;
        }
    }
}
