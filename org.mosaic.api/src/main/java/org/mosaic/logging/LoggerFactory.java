package org.mosaic.logging;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class LoggerFactory {

    private final static Map<String, SoftReference<Logger>> LOGGERS =
            new ConcurrentHashMap<String, SoftReference<Logger>>( 1500, 0.75f, 32 );

    @Nonnull
    public static Logger getLogger( @Nullable Class<?> clazz ) {
        return getLogger( clazz == null ? "unknown-logger" : clazz.getName() );
    }

    @Nonnull
    public static Logger getLogger( @Nullable String name ) {
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
