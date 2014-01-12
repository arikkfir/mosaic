package org.mosaic.util.resource.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.mosaic.server.Server;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
final class PathWatcherManager
{
    private static final Logger LOG = LoggerFactory.getLogger( PathWatcherManager.class );

    @Nonnull
    private final Map<PathWatcher, WatcherAdapter> adapters = new HashMap<>();

    @Nullable
    private ServiceTracker<Server, Path> defaultRootTracker;

    @Nullable
    private ScheduledExecutorService executorService;

    public synchronized void open( @Nonnull final BundleContext bundleContext )
    {
        this.defaultRootTracker = new ServiceTracker<>( bundleContext, Server.class, new ServiceTrackerCustomizer<Server, Path>()
        {
            @Override
            public Path addingService( @Nonnull ServiceReference<Server> reference )
            {
                Server server = bundleContext.getService( reference );
                return server != null ? server.getHome() : null;
            }

            @Override
            public void modifiedService( @Nonnull ServiceReference<Server> reference, @Nonnull Path service )
            {
                // no-op
            }

            @Override
            public void removedService( @Nonnull ServiceReference<Server> reference, @Nonnull Path service )
            {
                // no-op
            }
        } );
        this.defaultRootTracker.open();

        this.executorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon( true )
                        .setNameFormat( "PathScanner-%d" ).build() );
        this.executorService.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    PathWatcherManager.this.scan( getRootsAndWatchers() );
                }
                catch( Exception e )
                {
                    LOG.error( "Path watcher error occurred: {}", e.getMessage(), e );
                }
            }
        }, 0, 1, TimeUnit.SECONDS );
    }

    public synchronized void close()
    {
        if( this.executorService != null )
        {
            this.executorService.shutdown();
            this.executorService = null;
        }

        ServiceTracker<Server, Path> tracker = this.defaultRootTracker;
        if( tracker != null )
        {
            tracker.close();
        }
        this.defaultRootTracker = null;
    }

    public synchronized void addPathWatcher( @Nonnull Path location,
                                             @Nullable String pattern,
                                             @Nonnull Set<PathEvent> events,
                                             @Nonnull PathWatcher watcher )
    {
        WatcherAdapter adapter = this.adapters.get( watcher );
        if( adapter != null )
        {
            adapter.setLocation( location, pattern );
            adapter.setEvents( events );
        }
        else
        {
            adapter = new WatcherAdapter( location, pattern, events, watcher );
            this.adapters.put( watcher, adapter );
        }
        scan( singletonMap( adapter.location, asList( adapter ) ) );
    }

    public synchronized void removePathWatcher( @Nonnull PathWatcher watcher )
    {
        this.adapters.remove( watcher );
    }

    private synchronized void scan( @Nonnull Map<Path, List<WatcherAdapter>> watchers )
    {
        for( Map.Entry<Path, List<WatcherAdapter>> entry : watchers.entrySet() )
        {
            Path root = entry.getKey();
            final List<WatcherAdapter> adapters = entry.getValue();
            LOG.debug( "Starting scan of '{}', watchers are: {}", root, adapters );

            try
            {
                final Map<WatcherAdapter, PathWatcherContextImpl> contexts = new HashMap<>();

                // notify of scan start
                for( WatcherAdapter adapter : adapters )
                {
                    PathWatcherContextImpl context = new PathWatcherContextImpl();
                    context.setFile( root );
                    context.setEvent( SCAN_STARTED );
                    adapter.scanStarted( context );

                    contexts.put( adapter, context );
                }

                // traverse the path, invoking all adapters that registered for this path for file creation/modification
                if( exists( root ) )
                {
                    walkFileTree( root, EnumSet.of( FOLLOW_LINKS ), MAX_VALUE, new SimpleFileVisitor<Path>()
                    {
                        @Nonnull
                        @Override
                        public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                                throws IOException
                        {
                            for( WatcherAdapter adapter : adapters )
                            {
                                PathWatcherContextImpl context = contexts.get( adapter );
                                context.setFile( file );
                                adapter.visitFile( context );
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    } );
                }

                // give a chance to all adapters to find files which have been deleted
                for( WatcherAdapter adapter : adapters )
                {
                    PathWatcherContextImpl context = contexts.get( adapter );
                    context.setEvent( DELETED );
                    adapter.visitDeletedFiles( context );
                }

                // notify of scan start
                for( WatcherAdapter adapter : adapters )
                {
                    PathWatcherContextImpl context = contexts.get( adapter );
                    context.setEvent( SCAN_FINISHED );
                    context.setFile( root );
                    adapter.scanFinished( context );
                }
            }
            catch( Exception e )
            {
                LOG.error( "An error occurred while scanning '{}': {}", root, e.getMessage(), e );
            }
            finally
            {
                LOG.debug( "Finished scan of '{}', watchers are: {}", root, adapters );
            }
        }
    }

    private Map<Path, List<WatcherAdapter>> getRootsAndWatchers()
    {
        Map<Path, List<WatcherAdapter>> roots = new LinkedHashMap<>();
        for( WatcherAdapter adapter : this.adapters.values() )
        {
            List<WatcherAdapter> adapters = roots.get( adapter.location );
            if( adapters == null )
            {
                adapters = new LinkedList<>();
                roots.put( adapter.location, adapters );
            }
            adapters.add( adapter );
        }
        return roots;
    }

    private class PathWatcherContextImpl implements PathWatcherContext
    {
        @Nonnull
        private final DateTime scanStart = DateTime.now();

        @Nonnull
        private Path file;

        @Nonnull
        private PathEvent event;

        @Nullable
        private MapEx<String, Object> attributes;

        @Override
        @Nonnull
        public DateTime getScanStart()
        {
            return scanStart;
        }

        @Nonnull
        @Override
        public Path getFile()
        {
            return this.file;
        }

        private void setFile( @Nonnull Path file )
        {
            this.file = file;
        }

        @Nonnull
        @Override
        public PathEvent getEvent()
        {
            return this.event;
        }

        private void setEvent( @Nonnull PathEvent event )
        {
            this.event = event;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getAttributes()
        {
            if( this.attributes == null )
            {
                this.attributes = new HashMapEx<>();
            }
            return this.attributes;
        }
    }

    private class WatcherAdapter
    {
        @Nonnull
        private final PathMatcher pathMatcher = new AntPathMatcher();

        @Nonnull
        private final PathWatcher watcher;

        @Nonnull
        private final Map<Path, DateTime> knownTimes = new HashMap<>();

        @Nonnull
        private Path location;

        @Nullable
        private String pattern;

        @Nonnull
        private Set<PathEvent> events;

        private WatcherAdapter( @Nonnull Path location,
                                @Nullable String pattern,
                                @Nonnull Set<PathEvent> events,
                                @Nonnull PathWatcher watcher )
        {
            this.watcher = watcher;
            setLocation( location, pattern );
            setEvents( events );
        }

        public void setLocation( @Nonnull Path location, @Nullable String pattern )
        {
            if( !location.isAbsolute() )
            {
                if( defaultRootTracker != null )
                {
                    Path defaultRoot = defaultRootTracker.getService();
                    if( defaultRoot != null )
                    {
                        location = defaultRoot.resolve( location );
                    }
                    else
                    {
                        throw new IllegalStateException( "could not find server home!" );
                    }
                }
                else
                {
                    throw new IllegalStateException( "could not find server home!" );
                }
            }
            this.location = location;
            this.pattern = pattern;
            this.knownTimes.clear();
        }

        public void setEvents( @Nonnull Set<PathEvent> events )
        {
            this.events = events;
        }

        public void scanStarted( @Nonnull PathWatcherContextImpl context )
        {
            if( this.events.contains( SCAN_STARTED ) )
            {
                try
                {
                    this.watcher.handle( context );
                }
                catch( Exception e )
                {
                    LOG.error( "Watcher '{}' threw an exception while processing scan-start event of '{}': {}",
                               this.watcher, context.getFile(), e.getMessage(), e );
                }
            }
        }

        public void scanFinished( @Nonnull PathWatcherContextImpl context )
        {
            if( this.events.contains( SCAN_FINISHED ) )
            {
                try
                {
                    this.watcher.handle( context );
                }
                catch( Exception e )
                {
                    LOG.error( "Watcher '{}' threw an exception while processing scan-finish event of '{}': {}",
                               this.watcher, context.getFile(), e.getMessage(), e );
                }
            }
        }

        public void visitFile( @Nonnull PathWatcherContextImpl context )
        {
            Path file = context.getFile();

            if( this.pattern != null && !this.pathMatcher.matches( this.pattern, file.toString() ) )
            {
                return;
            }

            DateTime fileModificationTime = getFileModificationTime( file );
            if( fileModificationTime == null || fileModificationTime.isAfter( context.getScanStart().minusMillis( 1500 ) ) )
            {
                return;
            }

            DateTime lastModificationTime = this.knownTimes.get( file );
            this.knownTimes.put( file, fileModificationTime );

            if( lastModificationTime == null )
            {
                if( this.events.contains( CREATED ) )
                {
                    try
                    {
                        context.setEvent( CREATED );
                        this.watcher.handle( context );
                    }
                    catch( Exception e )
                    {
                        LOG.error( "Watcher '{}' threw an exception while processing a new file at '{}': {}",
                                   this.watcher, file, e.getMessage(), e );
                    }
                }
            }
            else if( fileModificationTime.isAfter( lastModificationTime ) )
            {
                if( this.events.contains( MODIFIED ) )
                {
                    try
                    {
                        context.setEvent( MODIFIED );
                        this.watcher.handle( context );
                    }
                    catch( Exception e )
                    {
                        LOG.error( "Watcher '{}' threw an exception while processing a modified file at '{}': {}",
                                   this.watcher, file, e.getMessage(), e );
                    }
                }
            }
            else
            {
                if( this.events.contains( NOT_MODIFIED ) )
                {
                    try
                    {
                        context.setEvent( NOT_MODIFIED );
                        this.watcher.handle( context );
                    }
                    catch( Exception e )
                    {
                        LOG.error( "Watcher '{}' threw an exception while processing an un-modified file at '{}': {}",
                                   this.watcher, file, e.getMessage(), e );
                    }
                }
            }
        }

        public void visitDeletedFiles( @Nonnull PathWatcherContextImpl context )
        {
            Iterator<Path> iterator = this.knownTimes.keySet().iterator();
            while( iterator.hasNext() )
            {
                Path file = iterator.next();
                context.setFile( file );

                if( Files.isDirectory( file ) )
                {
                    // a previous file has become a directory - remove it
                    iterator.remove();
                    LOG.warn( "A watched file at '{}' has been replaced by a directory - this even is not supported and will not be propagated to the path watcher" );
                }
                else if( Files.notExists( file ) )
                {
                    // file has been deleted
                    iterator.remove();
                    if( this.events.contains( DELETED ) )
                    {
                        try
                        {
                            this.watcher.handle( context );
                        }
                        catch( Exception e )
                        {
                            LOG.error( "Watcher '{}' threw an exception while processing a deleted file at '{}': {}",
                                       this.watcher, file, e.getMessage(), e );
                        }
                    }
                }
            }
        }

        @Nullable
        private DateTime getFileModificationTime( Path file )
        {
            try
            {
                long fileModificationTimeInMillis = Files.getLastModifiedTime( file ).toMillis();

                // ignore files modified in the last second (probably still being modified)
                if( fileModificationTimeInMillis < System.currentTimeMillis() - 1000 )
                {
                    return new DateTime( fileModificationTimeInMillis );
                }
            }
            catch( IOException e )
            {
                LOG.warn( "Could not obtain file modification time for '{}': {}", file, e.getMessage(), e );
            }
            return null;
        }
    }
}
