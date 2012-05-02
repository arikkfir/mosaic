package org.mosaic.util.logging;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author arik
 */
public class LoggerFactory
{

    private final static Map<String, SoftReference<Logger>> LOGGERS = new ConcurrentHashMap<>( 1500, 0.75f, 32 );

    public static Logger getBundleLogger( Class<?> clazz )
    {
        if( clazz == null )
        {
            return getLogger( "unknown-logger" );
        }
        Bundle bundle = FrameworkUtil.getBundle( clazz );
        return bundle != null ? getLogger( bundle.getSymbolicName( ) ) : getLogger( clazz.getName( ) );
    }

    public static Logger getBundleLogger( Class<?> clazz, String ext )
    {
        if( clazz == null )
        {
            return getLogger( "unknown-logger" );
        }

        Bundle bundle = FrameworkUtil.getBundle( clazz );
        Logger logger;
        if( bundle != null )
        {
            return getLogger( bundle.getSymbolicName( ) + "." + ext );
        }
        else
        {
            return getLogger( clazz.getName( ) + "." + ext );
        }
    }

    public static Logger getLogger( Class<?> clazz )
    {
        return getLogger( clazz == null ? "unknown-logger" : clazz.getName( ) );
    }

    public static Logger getLogger( String name )
    {
        SoftReference<Logger> loggerRef = LOGGERS.get( name );
        if( loggerRef != null )
        {
            Logger wrapper = loggerRef.get( );
            if( wrapper != null )
            {
                return wrapper;
            }
        }

        Logger wrapper = new Slf4jLoggerWrapper( org.slf4j.LoggerFactory.getLogger( name ) );
        LOGGERS.put( name, new SoftReference<>( wrapper ) );
        return wrapper;
    }
}
