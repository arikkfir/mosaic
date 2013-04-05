package org.mosaic.launcher.logging;

import ch.qos.logback.core.Appender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class AppenderRegistry
{
    private static final Map<String, Appender<?>> APPENDERS = new ConcurrentHashMap<>();

    public static final String FILE_APPENDER = "FILE";

    public static final String CONSOLE_APPENDER = "CONSOLE";

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E> Appender<E> findAppender( @Nullable String appenderName )
    {
        if( appenderName == null )
        {
            return null;
        }
        else
        {
            return ( Appender<E> ) APPENDERS.get( appenderName.toLowerCase() );
        }
    }

    @Nonnull
    public static <E> Appender<E> getDefaultAppender()
    {
        Appender<E> appender;
        if( Boolean.getBoolean( "dev" ) )
        {
            appender = findAppender( CONSOLE_APPENDER );
        }
        else
        {
            appender = findAppender( System.getProperty( "mosaic.logging.output", FILE_APPENDER ) );
        }

        if( appender == null )
        {
            throw new IllegalStateException( "Could not find default appender!" );
        }
        else
        {
            return appender;
        }
    }

    public void addAppender( @Nonnull Appender<?> appender )
    {
        APPENDERS.put( appender.getName().toLowerCase(), appender );
    }
}
