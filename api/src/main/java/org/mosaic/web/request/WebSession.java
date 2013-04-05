package org.mosaic.web.request;

import java.util.Date;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface WebSession
{
    @Nonnull
    String getId();

    @Nonnull
    Date getCreationTime();

    @Nonnull
    Date getLastAccessTime();

    long getMaxInactiveIntervalInSeconds();

    boolean isNew();

    void invalidate();
}
