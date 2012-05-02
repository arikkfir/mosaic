package org.mosaic.util.format;

/**
 * @author arik
 */
public class Formatter
{
    private static final ThreadLocal<java.util.Formatter> FORMATTER = new ThreadLocal<java.util.Formatter>( )
    {
        @Override
        protected java.util.Formatter initialValue( )
        {
            return new java.util.Formatter( new StringBuilder( 1000 ) );
        }
    };

    public static String format( String pattern, Object... args )
    {
        java.util.Formatter formatter = FORMATTER.get( );
        formatter.format( pattern, args );
        return formatter.toString( );
    }
}
