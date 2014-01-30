package org.mosaic.util.resource.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.properties.PropertyPlaceholderResolver;
import org.mosaic.util.resource.PathMatcher;
import org.mosaic.util.resource.PathWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.notExists;

/**
 * @author arik
 */
final class PathWatcherAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( PathWatcherAdapter.class );

    @Nonnull
    private final PathMatcher pathMatcher;

    @Nonnull
    private final PathWatcher watcher;

    @Nonnull
    private final Map<Path, DateTime> knownTimes = new ConcurrentHashMap<>();

    @Nonnull
    private Path location;

    @Nullable
    private String pattern;

    PathWatcherAdapter( @Nonnull PathMatcher pathMatcher,
                        @Nonnull ServiceReference<PathWatcher> reference,
                        @Nonnull PathWatcher pathWatcher )
    {
        this.pathMatcher = pathMatcher;
        this.watcher = pathWatcher;
        setProperties( reference );
    }

    @Nonnull
    Path getLocation()
    {
        return this.location;
    }

    void setProperties( @Nonnull final ServiceReference<PathWatcher> reference )
    {
        Object location = reference.getProperty( "location" );
        if( location == null )
        {
            throw new IllegalArgumentException( "Path watcher '" + this.watcher + "' did not provide a value for 'location' property" );
        }
        else if( location instanceof Path )
        {
            this.location = ( ( Path ) location ).toAbsolutePath().normalize();
        }
        else
        {
            PropertyPlaceholderResolver resolver = new PropertyPlaceholderResolver( new PropertyPlaceholderResolver.Resolver()
            {
                @Nullable
                @Override
                public String resolve( @Nonnull String propertyName )
                {
                    Bundle bundle = reference.getBundle();
                    if( bundle != null )
                    {
                        BundleContext bundleContext = bundle.getBundleContext();
                        if( bundleContext != null )
                        {
                            String value = bundleContext.getProperty( propertyName );
                            if( value != null )
                            {
                                return value;
                            }
                        }
                    }
                    return System.getProperty( propertyName );
                }
            } );
            this.location = Paths.get( resolver.resolve( location.toString() ) );
        }

        this.pattern = Objects.toString( reference.getProperty( "pattern" ), null );
        this.knownTimes.clear();
    }

    void scanStarted( @Nonnull MapEx<String, Object> context )
    {
        try
        {
            this.watcher.scanStarted( context );
        }
        catch( Exception e )
        {
            scanError( this.location, context, e );
        }
    }

    void scanError( @Nonnull Path path, @Nonnull MapEx<String, Object> context, @Nonnull Throwable e )
    {
        try
        {
            this.watcher.scanError( path, context, e );
        }
        catch( Exception e1 )
        {
            LOG.error( "Path watcher '{}' threw an exception while processing scan-error event of '{}': {}",
                       this.watcher, path, e1.getMessage(), e1 );
        }
    }

    public void visitFile( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // if this watcher defined a pattern, test this file against that pattern
        if( this.pattern != null && !this.pathMatcher.matches( this.pattern, path.toString() ) )
        {
            return;
        }

        // get file modification time
        DateTime fileModificationTime = getFileModificationTime( path );
        if( fileModificationTime == null )
        {
            return;
        }

        // get last known mod-time and store this new updated mod-time in our map
        DateTime lastModificationTime = this.knownTimes.get( path );
        this.knownTimes.put( path, fileModificationTime );

        // if this is a new file we did not know about before - file-created event
        if( lastModificationTime == null )
        {
            try
            {
                this.watcher.pathCreated( path, context );
            }
            catch( Exception e )
            {
                scanError( path, context, e );
            }
        }
        else if( fileModificationTime.isAfter( lastModificationTime ) )
        {
            try
            {
                this.watcher.pathModified( path, context );
            }
            catch( Exception e )
            {
                scanError( path, context, e );
            }
        }
        else
        {
            try
            {
                this.watcher.pathUnmodified( path, context );
            }
            catch( Exception e )
            {
                scanError( path, context, e );
            }
        }
    }

    void visitDeletedFiles( @Nonnull MapEx<String, Object> context )
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
                    this.watcher.pathDeleted( path, context );
                }
                catch( Exception e )
                {
                    scanError( path, context, e );
                }
            }
        }
    }

    void scanCompleted( @Nonnull MapEx<String, Object> context )
    {
        try
        {
            this.watcher.scanCompleted( context );
        }
        catch( Exception e )
        {
            scanError( this.location, context, e );
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
