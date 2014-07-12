package org.mosaic.core.modules.impl;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;
import static java.util.Objects.requireNonNull;

/**
 * @author arik
 */
public class ModuleWatcher
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ModuleWatcher.class );

    @Nonnull
    private final ServerImpl server;

    @Nullable
    private ScheduledExecutorService executorService;

    public ModuleWatcher( @Nonnull ServerImpl server )
    {
        this.server = server;

        // perform the startup scan
        server.addStartupHook( bundleContext -> scan() );

        // start watcher thread
        server.addStartupHook( bundleContext -> {
            LOG.debug( "Initializing module watcher on {}", this.server.getLib() );

            this.executorService = Executors.newSingleThreadScheduledExecutor( r -> {
                Thread thread = new Thread( r, "PathScanner" );
                thread.setDaemon( false );
                return thread;
            } );

            this.executorService.scheduleWithFixedDelay( this::scan, 0, 1, TimeUnit.SECONDS );
        } );

        server.addShutdownHook( bundleContext -> {
            LOG.debug( "Shutting down module watcher" );

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
        } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    synchronized void scan()
    {
        final Context context = new Context();

        // prepare for a new scan
        context.start();

        // scan existing files
        try
        {
            final PathMatcher jarMatcher = this.server.getLib().getFileSystem().getPathMatcher( "glob:**/*.jar" );
            walkFileTree( this.server.getLib(), EnumSet.of( FOLLOW_LINKS ), MAX_VALUE, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult preVisitDirectory( @Nonnull Path dir, @Nonnull BasicFileAttributes attrs )
                        throws IOException
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
            LOG.error( "Modules scanning error: {}", e.getMessage(), e );
        }

        try
        {
            // finish & apply scan results
            context.finish();
        }
        catch( Throwable e )
        {
            LOG.error( "Scan error", e );
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
            this.bundleContext = requireNonNull( ModuleWatcher.this.server.getBundleContext(), "could not find Mosaic bundle context" );
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
                bundle = this.bundleContext.getBundle( "reference:file:" + path.toString() );
            }

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
            LOG.error( message, args );
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
            frameworkWiring.getDependencyClosure( bundles )
                           .stream()
                           .filter( bundle -> bundle.getState() == Bundle.ACTIVE )
                           .forEach( bundle -> {
                               try
                               {
                                   bundle.stop();
                               }
                               catch( BundleException e )
                               {
                                   handleFileError( "Could not stop module from '{}': {}", bundle.getLocation(), e.getMessage(), e );
                               }
                               this.modulesToStart = addToList( this.modulesToStart, bundle );
                           } );

            AtomicBoolean done = new AtomicBoolean( false );
            frameworkWiring.refreshBundles( bundles, event -> done.set( true ) );
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
                        if( isDirectory( path ) )
                        {
                            //reference:file:/foo/bundle/
                            this.modulesToStart = addToList(
                                    this.modulesToStart,
                                    this.bundleContext.installBundle( "reference:file:" + path.toString() )
                            );
                        }
                        else
                        {
                            this.modulesToStart = addToList(
                                    this.modulesToStart,
                                    this.bundleContext.installBundle( "file:" + path.toString() )
                            );
                        }
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
