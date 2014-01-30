package org.mosaic.util.resource;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface PathWatcher
{
    void scanStarted( @Nonnull MapEx<String, Object> context );

    void pathCreated( @Nonnull Path path, @Nonnull MapEx<String, Object> context );

    void pathModified( @Nonnull Path path, @Nonnull MapEx<String, Object> context );

    void pathUnmodified( @Nonnull Path path, @Nonnull MapEx<String, Object> context );

    void pathDeleted( @Nonnull Path path, @Nonnull MapEx<String, Object> context );

    void scanError( @Nullable Path path, @Nonnull MapEx<String, Object> context, @Nonnull Throwable throwable );

    void scanCompleted( @Nonnull MapEx<String, Object> context );
}
