package org.mosaic.filewatch.impl.manager;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.Server;
import org.mosaic.filewatch.WatchEvent;
import org.mosaic.filewatch.annotation.FileWatcher;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.convert.ConversionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Integer.getInteger;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.framework.FrameworkUtil.createFilter;

/**
 * @author arik
 */
public class FileWatchersManager implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger( FileWatchersManager.class );

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final Server server;

    @Nonnull
    private final BundlesManager bundlesManager;

    @Nonnull
    private final ConversionService conversionService;

    @Nonnull
    private final ServiceTracker<MethodEndpoint, FileWatcherAdapter> visitorsTracker;

    @Nullable
    private ScheduledExecutorService scheduler;

    public FileWatchersManager( @Nonnull BundleContext bundleContext,
                                @Nonnull Server server,
                                @Nonnull BundlesManager bundlesManager,
                                @Nonnull ConversionService conversionService )
    {
        this.bundleContext = bundleContext;
        this.server = server;
        this.bundlesManager = bundlesManager;
        this.conversionService = conversionService;

        // create our file-visitors tracker
        try
        {
            String filter = format( "(&(objectClass=%s)(type=%s))", MethodEndpoint.class.getName(), FileWatcher.class.getName() );
            this.visitorsTracker = new ServiceTracker<>( this.bundleContext,
                                                         createFilter( filter ),
                                                         new FileWatcherEndpointCustomizer() );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalStateException( "Could not create a file-watcher filter from '" + e.getFilter() + "': " + e.getMessage(), e );
        }
    }

    @PostConstruct
    public void start()
    {
        // start tracking all known visitors
        this.visitorsTracker.open();

        // schedule a pass every second
        this.scheduler = newSingleThreadScheduledExecutor( new FileVisitorThreadFactory() );
        this.scheduler.scheduleWithFixedDelay( this, 1, 1, SECONDS );
    }

    @Override
    public void run()
    {
        scan();
    }

    @PreDestroy
    public void stop()
    {
        // shutdown our scheduler so no more scans will occur
        if( this.scheduler != null )
        {
            this.scheduler.shutdownNow();
        }
        this.scheduler = null;

        // close our visitors tracker
        this.visitorsTracker.close();
    }

    private synchronized void scan()
    {
        Multimap<Path, AbstractFileWatcherAdapter> adaptersByRoot = LinkedHashMultimap.create( 100, 5 );
        for( FileWatcherAdapter adapter : this.visitorsTracker.getTracked().values() )
        {
            adaptersByRoot.put( adapter.root, adapter );
        }
        adaptersByRoot.put( this.bundlesManager.getRoot(), this.bundlesManager );

        final ScanContext context = new ScanContext( this.conversionService );

        for( Map.Entry<Path, Collection<AbstractFileWatcherAdapter>> entry : adaptersByRoot.asMap().entrySet() )
        {
            Path root = entry.getKey();
            final Collection<AbstractFileWatcherAdapter> adapters = entry.getValue();

            // dispatch "scan-starting" event
            for( AbstractFileWatcherAdapter adapter : adapters )
            {
                adapter.scanStarting( context );
            }

            // walk the path tree
            if( exists( root ) )
            {
                if( isDirectory( root ) )
                {
                    try
                    {
                        walkFileTree( root, EnumSet.of( FOLLOW_LINKS ), 1024, new FileVisitor<Path>()
                        {
                            @Nonnull
                            @Override
                            public FileVisitResult preVisitDirectory( @Nonnull Path dir,
                                                                      @Nonnull BasicFileAttributes attrs )
                                    throws IOException
                            {
                                boolean svnDir = dir.getFileName().toString().equalsIgnoreCase( ".svn" );

                                for( AbstractFileWatcherAdapter adapter : adapters )
                                {
                                    if( !adapter.matchesSvnDir() || !svnDir )
                                    {
                                        adapter.handleDirectoryEnter( context, dir, attrs );
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Nonnull
                            @Override
                            public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                                    throws IOException
                            {
                                // take the actual up-to-date modification time; ignore files modified less than 2 seconds ago
                                // because they might still being written to...
                                long fileModTime = attrs.lastModifiedTime().toMillis();
                                if( fileModTime >= currentTimeMillis() - getInteger( "fileModificationGuard", 2000 ) )
                                {
                                    // file modified in the last two seconds, so skip, probably file is being written to...
                                    return FileVisitResult.CONTINUE;
                                }

                                for( AbstractFileWatcherAdapter adapter : adapters )
                                {
                                    adapter.handleExistingFile( context, file, attrs );
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Nonnull
                            @Override
                            public FileVisitResult visitFileFailed( @Nonnull Path file, @Nonnull IOException exc )
                                    throws IOException
                            {
                                LOG.error( "Could not inspect file '{}' for file watchers. Error is: {}", file, exc.getMessage(), exc );
                                return FileVisitResult.CONTINUE;
                            }

                            @Nonnull
                            @Override
                            public FileVisitResult postVisitDirectory( @Nonnull Path dir, @Nullable IOException exc )
                                    throws IOException
                            {
                                for( AbstractFileWatcherAdapter adapter : adapters )
                                {
                                    adapter.handleDirectoryExit( context, dir );
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        } );
                    }
                    catch( IOException e )
                    {
                        // should not happen since each visit method catches all exceptions - but just in case :)
                        LOG.error( "Could not walk file root '{}'. Error is: {}", root, e.getMessage(), e );
                    }
                }
                else if( isReadable( root ) )
                {
                    try
                    {
                        BasicFileAttributes attrs = Files.readAttributes( root, BasicFileAttributes.class );

                        // take the actual up-to-date modification time; ignore files modified less than 2 seconds ago
                        // because they might still being written to...
                        long fileModTime = attrs.lastModifiedTime().toMillis();
                        if( fileModTime < currentTimeMillis() - getInteger( "fileModificationGuard", 2000 ) )
                        {
                            for( AbstractFileWatcherAdapter adapter : adapters )
                            {
                                adapter.handleExistingFile( context, root, attrs );
                            }
                        }
                    }
                    catch( IOException e )
                    {
                        LOG.error( "Could not process file root '{}'. Error is: {}", root, e.getMessage(), e );
                    }
                }
            }

            // dispatch "scan-finished" event
            for( AbstractFileWatcherAdapter adapter : adapters )
            {
                adapter.scanFinished( context );
            }
        }
    }

    private class FileWatcherAdapter extends AbstractFileWatcherAdapter
    {
        @Nonnull
        private final MethodEndpoint endpoint;

        @Nonnull
        private final MethodEndpoint.Invoker invoker;

        @Nonnull
        private final Path root;

        @Nullable
        private final PathMatcher matcher;

        @Nonnull
        private final Set<WatchEvent> events;

        private final boolean skipSvn;

        private FileWatcherAdapter( @Nonnull MethodEndpoint endpoint )
        {
            this.endpoint = endpoint;
            this.invoker = this.endpoint.createInvoker( new WatcherParameterResolver() );

            FileWatcher ann = ( FileWatcher ) this.endpoint.getType();
            this.root = ann.root().getPath( server );
            this.skipSvn = ann.skipSvn();
            this.events = unmodifiableSet( newHashSet( ann.event().length == 0 ? WatchEvent.values() : ann.event() ) );
            this.matcher = ann.pattern().isEmpty() ? null : this.root.getFileSystem().getPathMatcher( "glob:" + ann.pattern() );
        }

        @Override
        protected boolean matchesEvent( @Nonnull WatchEvent event )
        {
            return this.events.contains( event );
        }

        @Override
        protected boolean matchesSvnDir()
        {
            return this.skipSvn;
        }

        @Nonnull
        @Override
        protected Path getRoot()
        {
            return this.root;
        }

        @Nullable
        @Override
        protected PathMatcher getPathMatcher()
        {
            return this.matcher;
        }

        @Override
        protected void notify( @Nonnull ScanContext scanContext,
                               @Nonnull WatchEvent event,
                               @Nullable Path path,
                               @Nullable BasicFileAttributes attrs )
        {
            Map<String, Object> context = new HashMap<>();
            context.put( "event", event );
            context.put( "root", getRoot() );
            context.put( "path", path );
            context.put( "attrs", attrs );
            try
            {
                this.invoker.resolve( context ).invoke();
            }
            catch( Exception e )
            {
                LOG.error( "File watcher '{}' threw an exception while processing {} event: {}", this.endpoint, event, e.getMessage(), e );
            }
        }
    }

    private class FileWatcherEndpointCustomizer implements ServiceTrackerCustomizer<MethodEndpoint, FileWatcherAdapter>
    {
        @Override
        public FileWatcherAdapter addingService( @Nonnull ServiceReference<MethodEndpoint> reference )
        {
            MethodEndpoint endpoint = bundleContext.getService( reference );
            if( endpoint != null )
            {
                FileWatcherAdapter adapter = new FileWatcherAdapter( endpoint );
                try
                {
                    scan();
                    return adapter;
                }
                catch( Exception e )
                {
                    LOG.warn( "Could not register file visitor: {}", e.getMessage(), e );
                    return null;
                }
            }
            else
            {
                return null;
            }
        }

        @Override
        public void modifiedService( @Nonnull ServiceReference<MethodEndpoint> reference,
                                     @Nonnull FileWatcherAdapter service )
        {
            // no-op
        }

        @Override
        public void removedService( @Nonnull ServiceReference<MethodEndpoint> reference,
                                    @Nonnull FileWatcherAdapter service )
        {
            // no-op
        }
    }
}
