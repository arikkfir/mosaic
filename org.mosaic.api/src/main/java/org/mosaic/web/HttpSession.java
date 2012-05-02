package org.mosaic.web;

import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * @author arik
 */
public interface HttpSession extends Map<String, Object>
{
    DateTime getCreationTime();

    String getId();

    DateTime getLastAccessTime();

    Duration getMaxInactiveInterval();

    boolean isNew();

    void invalidate();
}
