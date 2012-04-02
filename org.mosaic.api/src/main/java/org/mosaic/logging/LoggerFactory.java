package org.mosaic.logging;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author arik
 */
public class LoggerFactory {

    private final static Map<String, SoftReference<Logger>> LOGGERS = new ConcurrentHashMap<>( 1500, 0.75f, 32 );

    public static Logger getLogger( Class<?> clazz ) {
        return getLogger( clazz == null ? "unknown-logger" : clazz.getName() );
    }

    public static Logger getLogger( String name ) {
        SoftReference<Logger> loggerRef = LOGGERS.get( name );
        if( loggerRef != null ) {
            Logger wrapper = loggerRef.get();
            if( wrapper != null ) {
                return wrapper;
            }
        }

        Logger wrapper = new Slf4jLoggerWrapper( org.slf4j.LoggerFactory.getLogger( name ) );
        LOGGERS.put( name, new SoftReference<>( wrapper ) );
        return wrapper;
    }
}
