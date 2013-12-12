package org.mosaic.util.resource;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface PathWatcherContext
{
    @Nonnull
    DateTime getScanStart();

    @Nonnull
    Path getFile();

    @Nonnull
    PathEvent getEvent();

    @Nonnull
    MapEx<String, Object> getAttributes();
}
