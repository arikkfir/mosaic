package org.mosaic.web;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collection.TypedDict;

/**
 * @author arik
 */
public interface HttpSession extends TypedDict<Object> {

    DateTime getCreationTime( );

    String getId( );

    DateTime getLastAccessTime( );

    Duration getMaxInactiveInterval( );

    boolean isNew( );

    void invalidate( );

}
