package org.mosaic.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;

/**
 * @author arik
 */
final class EventsLogger
{
    private static final Logger LOG = LoggerFactory.getLogger( "org.mosaic.events" );

    private static final String SEPARATOR = "******************************************************************************";

    public static void printEmphasizedInfoMessage( String message, Object... args )
    {
        FormattingTuple tuple = arrayFormat( message, args );
        LOG.info( "\n\n" + SEPARATOR + "\n" + tuple.getMessage() + "\n" + SEPARATOR + "\n", tuple.getThrowable() );
    }

    public static void printEmphasizedWarnMessage( String message, Object... args )
    {
        FormattingTuple tuple = arrayFormat( message, args );
        LOG.warn( "\n\n" + SEPARATOR + "\n" + tuple.getMessage() + "\n" + SEPARATOR + "\n", tuple.getThrowable() );
    }

    public static void printEmphasizedErrorMessage( String message, Object... args )
    {
        FormattingTuple tuple = arrayFormat( message, args );
        LOG.error( "\n\n" + SEPARATOR + "\n" + tuple.getMessage() + "\n" + SEPARATOR + "\n", tuple.getThrowable() );
    }

    private EventsLogger()
    {
    }
}
