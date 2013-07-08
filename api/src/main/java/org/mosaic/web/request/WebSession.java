package org.mosaic.web.request;

import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface WebSession
{
    @Nonnull
    MapEx<String, Object> getAttributes();

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
}
