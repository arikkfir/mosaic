package org.mosaic.web.request;

import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface WebSession
{
    @Nonnull
    String getId();

    @Nonnull
    DateTime getCreationTime();

    @Nonnull
    DateTime getLastAccessTime();

    @Nonnull
    Duration getMaxInactiveIntervalInSeconds();

    boolean isNew();

    void invalidate();

    @Nonnull
    MapEx<String, Object> getAttributes();
}
