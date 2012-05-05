package org.mosaic.web;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collection.MapAccessor;

/**
 * @author arik
 */
public interface HttpSession extends MapAccessor<String, Object>
{
    DateTime getCreationTime();

    String getId();

    DateTime getLastAccessTime();

    Duration getMaxInactiveInterval();

    boolean isNew();

    void invalidate();
}
