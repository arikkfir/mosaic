package org.mosaic.web;

import java.text.ParseException;
import java.util.Locale;
import org.joda.time.DateTime;
import org.mosaic.web.util.HttpTime;
import org.springframework.format.Formatter;

/**
 * @author arik
 */
public class DateTimeFormatter implements Formatter<DateTime> {

    @Override
    public DateTime parse( String text, Locale locale ) throws ParseException {
        return HttpTime.parse( text );
    }

    @Override
    public String print( DateTime object, Locale locale ) {
        return HttpTime.format( object );
    }
}
