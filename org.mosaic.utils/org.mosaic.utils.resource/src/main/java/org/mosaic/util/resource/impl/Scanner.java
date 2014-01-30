package org.mosaic.util.resource.impl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import javax.annotation.Nonnull;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.PathMatcher;
import org.mosaic.util.resource.PathWatcher;
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

/**
 * @author arik
 */
final class Scanner
{
    private static final Logger LOG = LoggerFactory.getLogger( Scanner.class );

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final PathMatcher pathMatcher;

    @Nonnull
    private final ServiceTracker<PathWatcher, PathWatcherAdapter> pathWatchersTracker;

    Scanner( @Nonnull BundleContext bundleContext, @Nonnull PathMatcher pathMatcher )
    {
        this.bundleContext = bundleContext;
        this.pathMatcher = pathMatcher;
        this.pathWatchersTracker = new ServiceTracker<>( bundleContext,
                                                         PathWatcher.class,
                                                         new PathWatcherCustomizer() );
    }

    void open()
    {
        this.pathWatchersTracker.open();
    }

    void close()
    {
        this.pathWatchersTracker.close();
    }

    synchronized void scan()
    {
        Map<Path, List<PathWatcherAdapter>> roots = new LinkedHashMap<>();
        for( PathWatcherAdapter adapter : this.pathWatchersTracker.getTracked().values() )
        {
            List<PathWatcherAdapter> adapters = roots.get( adapter.getLocation() );
            if( adapters == null )
            {
                adapters = new LinkedList<>();
                roots.put( adapter.getLocation(), adapters );
            }
            adapters.add( adapter );
        }
        scan( roots );
    }

    private synchronized void scan( @Nonnull Map<Path, List<PathWatcherAdapter>> watchers )
    {
        for( Map.Entry<Path, List<PathWatcherAdapter>> entry : watchers.entrySet() )
        {
            Path root = entry.getKey();
            final List<PathWatcherAdapter> adapters = entry.getValue();
            LOG.debug( "Starting scan of '{}', watchers are: {}", root, adapters );
            try
            {
                final Map<PathWatcherAdapter, MapEx<String, Object>> contexts = new HashMap<>();

                // notify of scan start
                for( PathWatcherAdapter adapter : adapters )
                {
                    MapEx<String, Object> context = new HashMapEx<>();
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
                            for( PathWatcherAdapter adapter : adapters )
                            {
                                adapter.visitFile( file, contexts.get( adapter ) );
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    } );
                }

                // give a chance to all adapters to find files which have been deleted
                for( PathWatcherAdapter adapter : adapters )
                {
                    adapter.visitDeletedFiles( contexts.get( adapter ) );
                }

                // notify of scan start
                for( PathWatcherAdapter adapter : adapters )
                {
                    adapter.scanCompleted( contexts.get( adapter ) );
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

    private class PathWatcherCustomizer implements ServiceTrackerCustomizer<PathWatcher, PathWatcherAdapter>
    {
        @Override
        public PathWatcherAdapter addingService( @Nonnull ServiceReference<PathWatcher> reference )
        {
            PathWatcher pathWatcher = bundleContext.getService( reference );
            if( pathWatcher != null )
            {
                PathWatcherAdapter adapter = new PathWatcherAdapter( pathMatcher, reference, pathWatcher );
                scan( singletonMap( adapter.getLocation(), asList( adapter ) ) );
                return adapter;
            }
            else
            {
                return null;
            }
        }

        @Override
        public void modifiedService( @Nonnull ServiceReference<PathWatcher> reference,
                                     @Nonnull PathWatcherAdapter service )
        {
            service.setProperties( reference );
        }

        @Override
        public void removedService( @Nonnull ServiceReference<PathWatcher> reference,
                                    @Nonnull PathWatcherAdapter service )
        {
            // no-op
        }
    }
}
