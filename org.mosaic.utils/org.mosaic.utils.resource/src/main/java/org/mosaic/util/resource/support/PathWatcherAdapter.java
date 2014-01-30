package org.mosaic.util.resource.support;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.resource.PathWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class PathWatcherAdapter implements PathWatcher
{
    @Override
    public void scanStarted( @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathCreated( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathModified( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathUnmodified( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathDeleted( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void scanError( @Nullable Path path, @Nonnull MapEx<String, Object> context, @Nonnull Throwable throwable )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.warn( "Path watcher '{}' threw an error while handling '{}': {}", this, path, throwable.getMessage(), throwable );
    }

    @Override
    public void scanCompleted( @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }
}
