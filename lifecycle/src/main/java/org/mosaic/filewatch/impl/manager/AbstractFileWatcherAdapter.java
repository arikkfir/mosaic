package org.mosaic.filewatch.impl.manager;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.filewatch.WatchEvent;

import static java.nio.file.Files.notExists;
import static org.mosaic.filewatch.WatchEvent.*;

/**
 * @author arik
 */
public abstract class AbstractFileWatcherAdapter
{
    @Nonnull
    private final Map<Path, Long> knownFiles = new HashMap<>();

    public void scanStarting( @Nonnull ScanContext context )
    {
        if( matchesEvent( SCAN_STARTING, null ) )
        {
            notify( context, SCAN_STARTING, null, null );
        }
    }

    public void handleDirectoryEnter( @Nonnull ScanContext context, @Nonnull Path path, BasicFileAttributes attrs )
    {
        if( matchesEvent( DIR_ENTER, path ) )
        {
            notify( context, DIR_ENTER, path, attrs );
        }
    }

    public void handleExistingFile( @Nonnull ScanContext context, @Nonnull Path path, BasicFileAttributes attrs )
    {
        if( !matches( path ) )
        {
            return;
        }

        // first get the known file modification time (before we "know" about this file)
        Long knownModTime = this.knownFiles.get( path );

        // now update our known file modification times for the next iterations
        long fileModTime = attrs.lastModifiedTime().toMillis();
        this.knownFiles.put( path, fileModTime );
        if( knownModTime != null )
        {
            if( matchesEvent( FILE_MODIFIED, path ) && fileModTime > knownModTime )
            {
                notify( context, FILE_MODIFIED, path, attrs );
            }
        }
        else if( matchesEvent( FILE_ADDED, path ) )
        {
            notify( context, FILE_ADDED, path, attrs );
        }
    }

    public void handleDirectoryExit( @Nonnull ScanContext context, @Nonnull Path path )
    {
        // detect file deletions
        if( matchesEvent( FILE_DELETED, path ) )
        {
            for( Iterator<Path> iterator = this.knownFiles.keySet().iterator(); iterator.hasNext(); )
            {
                Path file = iterator.next();
                if( notExists( file ) && matches( file ) )
                {
                    // file was deleted - remove it from the known file modifications map, and notify the adapter
                    iterator.remove();

                    // invoke watcher
                    notify( context, FILE_DELETED, path, null );
                }
            }
        }

        if( matchesEvent( DIR_EXIT, path ) )
        {
            notify( context, DIR_EXIT, path, null );
        }
    }

    public void scanFinished( @Nonnull ScanContext context )
    {
        if( matchesEvent( SCAN_FINISHED, null ) )
        {
            notify( context, SCAN_FINISHED, null, null );
        }
    }

    protected boolean matchesEvent( @Nonnull WatchEvent event )
    {
        return false;
    }

    protected boolean matchesEvent( @Nonnull WatchEvent event, @Nullable Path path )
    {
        return matchesEvent( event );
    }

    protected abstract boolean matchesSvnDir();

    @Nonnull
    protected abstract Path getRoot();

    @Nullable
    protected abstract PathMatcher getPathMatcher();

    protected abstract void notify( @Nonnull ScanContext context,
                                    @Nonnull WatchEvent event,
                                    @Nullable Path path,
                                    @Nullable BasicFileAttributes attrs );

    private boolean matches( @Nonnull Path path )
    {
        PathMatcher matcher = getPathMatcher();
        if( getRoot().equals( path ) )
        {
            return matcher == null;
        }
        else if( !path.startsWith( getRoot() ) )
        {
            return false;
        }
        else if( matcher == null )
        {
            return true;
        }
        else
        {
            Path relative = getRoot().relativize( path );
            return matcher.matches( relative );
        }
    }
}
