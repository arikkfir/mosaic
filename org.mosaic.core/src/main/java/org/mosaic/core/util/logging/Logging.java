package org.mosaic.core.util.logging;

import org.mosaic.core.util.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * @author arik
 */
public final class Logging
{
    @Nonnull
    public static Logger getLogger()
    {
        RuntimeException runtimeException = new RuntimeException();
        StackTraceElement[] stackTrace = runtimeException.getStackTrace();
        for( StackTraceElement stackTraceElement : stackTrace )
        {
            String className = stackTraceElement.getClassName();
            if( !className.equals( Logging.class.getName() ) )
            {
                return getLogger( className );
            }
        }
        return getLogger( Logging.class );
    }

    @Nonnull
    public static Logger getLogger( @Nonnull Class<?> clazz )
    {
        return LoggerFactory.getLogger( clazz );
    }

    @Nonnull
    public static Logger getLogger( @Nonnull String clazz )
    {
        return LoggerFactory.getLogger( clazz );
    }

    @Nonnull
    public static Logger getMarkerLogger( @Nonnull String marker )
    {
        return getMarkerLogger( MarkerFactory.getMarker( marker ) );
    }

    @Nonnull
    public static Logger getMarkerLogger( @Nonnull Marker marker )
    {
        return getMarkerLogger( getLogger(), marker );
    }

    @Nonnull
    public static Logger getMarkerLogger( @Nonnull Logger logger, @Nonnull String marker )
    {
        return getMarkerLogger( logger, MarkerFactory.getMarker( marker ) );
    }

    @Nonnull
    public static Logger getMarkerLogger( final @Nonnull Logger logger, final @Nonnull Marker marker )
    {
        return new MarkerLogger( logger, marker );
    }

    private static class MarkerLogger implements Logger
    {
        @Nonnull
        private final Logger logger;

        @Nonnull
        private final Marker marker;

        public MarkerLogger( @Nonnull Logger logger, @Nonnull Marker marker )
        {
            this.logger = logger;
            this.marker = marker;
        }

        @Override
        public String getName()
        {
            return logger.getName();
        }

        @Override
        public boolean isTraceEnabled()
        {
            return logger.isTraceEnabled( marker );
        }

        @Override
        public void trace( String msg )
        {
            logger.trace( marker, msg );
        }

        @Override
        public void trace( String format, Object arg )
        {
            logger.trace( marker, format, arg );
        }

        @Override
        public void trace( String format, Object arg1, Object arg2 )
        {
            logger.trace( marker, format, arg1, arg2 );
        }

        @Override
        public void trace( String format, Object... arguments )
        {
            logger.trace( marker, format, arguments );
        }

        @Override
        public void trace( String msg, Throwable t )
        {
            logger.trace( marker, msg, t );
        }

        @Override
        public boolean isTraceEnabled( Marker marker )
        {
            return logger.isTraceEnabled( marker );
        }

        @Override
        public void trace( Marker marker, String msg )
        {
            logger.trace( marker, msg );
        }

        @Override
        public void trace( Marker marker, String format, Object arg )
        {
            logger.trace( marker, format, arg );
        }

        @Override
        public void trace( Marker marker, String format, Object arg1, Object arg2 )
        {
            logger.trace( marker, format, arg1, arg2 );
        }

        @Override
        public void trace( Marker marker, String format, Object... argArray )
        {
            logger.trace( marker, format, argArray );
        }

        @Override
        public void trace( Marker marker, String msg, Throwable t )
        {
            logger.trace( marker, msg, t );
        }

        @Override
        public boolean isDebugEnabled()
        {
            return logger.isDebugEnabled( marker );
        }

        @Override
        public void debug( String msg )
        {
            logger.debug( marker, msg );
        }

        @Override
        public void debug( String format, Object arg )
        {
            logger.debug( marker, format, arg );
        }

        @Override
        public void debug( String format, Object arg1, Object arg2 )
        {
            logger.debug( marker, format, arg1, arg2 );
        }

        @Override
        public void debug( String format, Object... arguments )
        {
            logger.debug( marker, format, arguments );
        }

        @Override
        public void debug( String msg, Throwable t )
        {
            logger.debug( marker, msg, t );
        }

        @Override
        public boolean isDebugEnabled( Marker marker )
        {
            return logger.isDebugEnabled( marker );
        }

        @Override
        public void debug( Marker marker, String msg )
        {
            logger.debug( marker, msg );
        }

        @Override
        public void debug( Marker marker, String format, Object arg )
        {
            logger.debug( marker, format, arg );
        }

        @Override
        public void debug( Marker marker, String format, Object arg1, Object arg2 )
        {
            logger.debug( marker, format, arg1, arg2 );
        }

        @Override
        public void debug( Marker marker, String format, Object... arguments )
        {
            logger.debug( marker, format, arguments );
        }

        @Override
        public void debug( Marker marker, String msg, Throwable t )
        {
            logger.debug( marker, msg, t );
        }

        @Override
        public boolean isInfoEnabled()
        {
            return logger.isInfoEnabled( marker );
        }

        @Override
        public void info( String msg )
        {
            logger.info( marker, msg );
        }

        @Override
        public void info( String format, Object arg )
        {
            logger.info( marker, format, arg );
        }

        @Override
        public void info( String format, Object arg1, Object arg2 )
        {
            logger.info( marker, format, arg1, arg2 );
        }

        @Override
        public void info( String format, Object... arguments )
        {
            logger.info( marker, format, arguments );
        }

        @Override
        public void info( String msg, Throwable t )
        {
            logger.info( marker, msg, t );
        }

        @Override
        public boolean isInfoEnabled( Marker marker )
        {
            return logger.isInfoEnabled( marker );
        }

        @Override
        public void info( Marker marker, String msg )
        {
            logger.info( marker, msg );
        }

        @Override
        public void info( Marker marker, String format, Object arg )
        {
            logger.info( marker, format, arg );
        }

        @Override
        public void info( Marker marker, String format, Object arg1, Object arg2 )
        {
            logger.info( marker, format, arg1, arg2 );
        }

        @Override
        public void info( Marker marker, String format, Object... arguments )
        {
            logger.info( marker, format, arguments );
        }

        @Override
        public void info( Marker marker, String msg, Throwable t )
        {
            logger.info( marker, msg, t );
        }

        @Override
        public boolean isWarnEnabled()
        {
            return logger.isWarnEnabled( marker );
        }

        @Override
        public void warn( String msg )
        {
            logger.warn( marker, msg );
        }

        @Override
        public void warn( String format, Object arg )
        {
            logger.warn( marker, format, arg );
        }

        @Override
        public void warn( String format, Object... arguments )
        {
            logger.warn( marker, format, arguments );
        }

        @Override
        public void warn( String format, Object arg1, Object arg2 )
        {
            logger.warn( marker, format, arg1, arg2 );
        }

        @Override
        public void warn( String msg, Throwable t )
        {
            logger.warn( marker, msg, t );
        }

        @Override
        public boolean isWarnEnabled( Marker marker )
        {
            return logger.isWarnEnabled( marker );
        }

        @Override
        public void warn( Marker marker, String msg )
        {
            logger.warn( marker, msg );
        }

        @Override
        public void warn( Marker marker, String format, Object arg )
        {
            logger.warn( marker, format, arg );
        }

        @Override
        public void warn( Marker marker, String format, Object arg1, Object arg2 )
        {
            logger.warn( marker, format, arg1, arg2 );
        }

        @Override
        public void warn( Marker marker, String format, Object... arguments )
        {
            logger.warn( marker, format, arguments );
        }

        @Override
        public void warn( Marker marker, String msg, Throwable t )
        {
            logger.warn( marker, msg, t );
        }

        @Override
        public boolean isErrorEnabled()
        {
            return logger.isErrorEnabled( marker );
        }

        @Override
        public void error( String msg )
        {
            logger.error( marker, msg );
        }

        @Override
        public void error( String format, Object arg )
        {
            logger.error( marker, format, arg );
        }

        @Override
        public void error( String format, Object arg1, Object arg2 )
        {
            logger.error( marker, format, arg1, arg2 );
        }

        @Override
        public void error( String format, Object... arguments )
        {
            logger.error( marker, format, arguments );
        }

        @Override
        public void error( String msg, Throwable t )
        {
            logger.error( marker, msg, t );
        }

        @Override
        public boolean isErrorEnabled( Marker marker )
        {
            return logger.isErrorEnabled( marker );
        }

        @Override
        public void error( Marker marker, String msg )
        {
            logger.error( marker, msg );
        }

        @Override
        public void error( Marker marker, String format, Object arg )
        {
            logger.error( marker, format, arg );
        }

        @Override
        public void error( Marker marker, String format, Object arg1, Object arg2 )
        {
            logger.error( marker, format, arg1, arg2 );
        }

        @Override
        public void error( Marker marker, String format, Object... arguments )
        {
            logger.error( marker, format, arguments );
        }

        @Override
        public void error( Marker marker, String msg, Throwable t )
        {
            logger.error( marker, msg, t );
        }
    }

    private Logging()
    {
    }
}
