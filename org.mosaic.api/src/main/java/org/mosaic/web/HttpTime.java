package org.mosaic.web;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.tz.FixedDateTimeZone;

import static java.util.Locale.ENGLISH;
import static org.joda.time.format.DateTimeFormat.forPattern;

/**
 * @author arik
 */
public abstract class HttpTime {

    public static final DateTimeZone ZONE = new FixedDateTimeZone( "GMT", "GMT", 0, 0 );

    public static final DateTimeFormatter ANSI_DATE_TIME_FORMATTER = formatter( "EEE MMM dd HH:mm:ss yyyy" );

    public static final DateTimeFormatter RFC_850_DATE_TIME_FORMATTER = formatter( "EEE, dd-MMM-yy HH:mm:ss" );

    public static final DateTimeFormatter RFC_1123_DATE_TIME_FORMATTER = formatter( "EEE, dd MMM yyyy HH:mm:ss" );

    public static DateTime parse( String httpTime ) {
        if( httpTime.contains( ";" ) ) {
            httpTime = httpTime.substring( 0, httpTime.indexOf( ';' ) );
        }

        try {
            return RFC_1123_DATE_TIME_FORMATTER.parseDateTime( httpTime.substring( 0, httpTime.length() - 4 ) ).withZone( ZONE );
        } catch( IllegalArgumentException ignore ) {
        }

        try {
            return RFC_850_DATE_TIME_FORMATTER.parseDateTime( httpTime ).withZone( ZONE );
        } catch( Exception ignore ) {
        }

        try {
            return ANSI_DATE_TIME_FORMATTER.parseDateTime( httpTime );
        } catch( Exception e ) {
            throw new IllegalArgumentException( "Could not parse HTTP date/time value '" + httpTime + "' - unrecognizable format" );
        }
    }

    private static DateTimeFormatter formatter( String pattern ) {
        return forPattern( pattern ).withZone( ZONE ).withLocale( ENGLISH );
    }

}
