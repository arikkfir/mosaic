package org.mosaic.util.logging;

import org.slf4j.MDC;

/**
 * @author arik
 */
public final class LogContext
{
    public static void put( String key, Object value )
    {
        MDC.put( key, value == null ? "null" : value.toString() );
    }

    public static void remove( String key )
    {
        MDC.remove( key );
    }

    private LogContext()
    {
    }
}
