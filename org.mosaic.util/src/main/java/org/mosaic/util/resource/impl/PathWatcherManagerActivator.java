package org.mosaic.util.resource.impl;

import com.google.common.collect.Sets;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.resource.PathEvent;
import org.mosaic.util.resource.PathWatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
public final class PathWatcherManagerActivator implements BundleActivator
{
    private static final Logger LOG = LoggerFactory.getLogger( PathWatcherManagerActivator.class );

    private static final PropertyPlaceholderResolver PROPERTY_PLACEHOLDER_RESOLVER = new PropertyPlaceholderResolver();

    @Nullable
    private PathWatcherManager pathWatcherManager;

    @Nullable
    private ServiceTracker<PathWatcher, PathWatcher> pathWatchersTracker;

    @Override
    public void start( @Nonnull final BundleContext bundleContext ) throws Exception
    {
        this.pathWatcherManager = new PathWatcherManager();
        this.pathWatcherManager.open( bundleContext );
        this.pathWatchersTracker = new ServiceTracker<>( bundleContext, PathWatcher.class, new PathWatcherServiceTrackerCustomizer( bundleContext ) );
        this.pathWatchersTracker.open();
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.pathWatchersTracker != null )
        {
            try
            {
                this.pathWatchersTracker.close();
            }
            catch( Exception ignore )
            {
            }
        }
        this.pathWatchersTracker = null;

        if( this.pathWatcherManager != null )
        {
            this.pathWatcherManager.close();
        }
        this.pathWatcherManager = null;
    }

    private class PathWatcherServiceTrackerCustomizer implements ServiceTrackerCustomizer<PathWatcher, PathWatcher>
    {
        private final BundleContext bundleContext;

        public PathWatcherServiceTrackerCustomizer( BundleContext bundleContext )
        {
            this.bundleContext = bundleContext;
        }

        @Override
        public synchronized PathWatcher addingService( @Nonnull ServiceReference<PathWatcher> reference )
        {
            PathWatcherManager pathWatcherManager = PathWatcherManagerActivator.this.pathWatcherManager;
            if( pathWatcherManager != null )
            {
                PathWatcher service = bundleContext.getService( reference );
                if( service != null && addUpdateWatcher( reference, service ) )
                {
                    return service;
                }
            }
            return null;
        }

        @Override
        public void modifiedService( @Nonnull ServiceReference<PathWatcher> reference,
                                     @Nonnull PathWatcher service )
        {
            addUpdateWatcher( reference, service );
        }

        @Override
        public synchronized void removedService( @Nonnull ServiceReference<PathWatcher> reference,
                                                 @Nonnull PathWatcher service )
        {
            PathWatcherManager pathWatcherManager = PathWatcherManagerActivator.this.pathWatcherManager;
            if( pathWatcherManager != null )
            {
                pathWatcherManager.removePathWatcher( service );
            }
        }

        private boolean addUpdateWatcher( @Nonnull ServiceReference<PathWatcher> reference,
                                          @Nonnull PathWatcher service )
        {
            PathWatcherManager pathWatcherManager = PathWatcherManagerActivator.this.pathWatcherManager;
            if( pathWatcherManager == null )
            {
                return false;
            }

            String location = Objects.toString( reference.getProperty( "location" ), null );
            if( location == null )
            {
                LOG.warn( "PathWatcher '{}' was registered from '{}[{}]-{}' without 'location' property (it will be ignored)",
                          service,
                          reference.getBundle().getSymbolicName(),
                          reference.getBundle().getBundleId(),
                          reference.getBundle().getVersion() );
                pathWatcherManager.removePathWatcher( service );
                return false;
            }
            else
            {
                location = PROPERTY_PLACEHOLDER_RESOLVER.resolve( location );
            }

            String pattern = Objects.toString( reference.getProperty( "pattern" ), null );
            if( pattern != null )
            {
                pattern = PROPERTY_PLACEHOLDER_RESOLVER.resolve( pattern );
            }

            Set<PathEvent> eventSet;
            Object events = reference.getProperty( "events" );
            if( events == null )
            {
                eventSet = Sets.newHashSet( CREATED, MODIFIED, DELETED );
            }
            else
            {
                eventSet = new HashSet<>();

                Class<?> eventsClass = events.getClass();
                if( !eventsClass.isArray() )
                {
                    LOG.warn( "The 'events' property of service '{}' is not an array - ignoring this watcher", service );
                    pathWatcherManager.removePathWatcher( service );
                    return false;
                }

                int length = Array.getLength( events );
                for( int i = 0; i < length; i++ )
                {
                    Object event = Array.get( events, i );
                    if( event instanceof PathEvent )
                    {
                        eventSet.add( ( PathEvent ) event );
                    }
                    else if( event != null )
                    {
                        String eventName = event.toString();
                        try
                        {
                            eventSet.add( PathEvent.valueOf( eventName ) );
                        }
                        catch( Throwable e )
                        {
                            LOG.warn( "Illegal event ('{}') registered as property 'events' of service '{}' for file watching - ignoring event",
                                      eventName, service );
                            pathWatcherManager.removePathWatcher( service );
                            return false;
                        }
                    }
                    else
                    {
                        LOG.warn( "Null event registered inside array property 'events' of service '{}' for file watching - ignoring event", service );
                        pathWatcherManager.removePathWatcher( service );
                        return false;
                    }
                }
            }

            pathWatcherManager.addPathWatcher( Paths.get( location ), pattern, eventSet, service );
            return true;
        }
    }
}
