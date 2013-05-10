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
        if( matchesEvent( SCAN_STARTING ) )
        {
            notify( context, SCAN_STARTING, null, null );
        }
    }

    public void handleDirectoryEnter( @Nonnull ScanContext context, @Nonnull Path path, BasicFileAttributes attrs )
    {
        if( matchesEvent( DIR_ENTER ) )
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

        long fileModTime = attrs.lastModifiedTime().toMillis();
        this.knownFiles.put( path, fileModTime );

        Long knownModTime = this.knownFiles.get( path );
        if( knownModTime != null )
        {
            if( matchesEvent( FILE_MODIFIED ) && fileModTime > knownModTime )
            {
                notify( context, FILE_MODIFIED, path, attrs );
            }
        }
        else if( matchesEvent( FILE_ADDED ) )
        {
            notify( context, FILE_ADDED, path, attrs );
        }
    }

    public void handleDirectoryExit( @Nonnull ScanContext context, @Nonnull Path path )
    {
        // detect file deletions
        if( matchesEvent( FILE_DELETED ) )
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

        if( matchesEvent( DIR_EXIT ) )
        {
            notify( context, DIR_EXIT, path, null );
        }
    }

    public void scanFinished( @Nonnull ScanContext context )
    {
        if( matchesEvent( SCAN_FINISHED ) )
        {
            notify( context, SCAN_FINISHED, null, null );
        }
    }

    protected abstract boolean matchesEvent( @Nonnull WatchEvent event );

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