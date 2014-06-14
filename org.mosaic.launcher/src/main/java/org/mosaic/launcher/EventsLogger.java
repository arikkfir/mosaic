package org.mosaic.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LOG.info( "\n\n" + SEPARATOR + "\n" + arrayFormat( message, args ).getMessage() + "\n" + SEPARATOR + "\n" );
    }

    public static void printEmphasizedWarnMessage( String message, Object... args )
    {
        LOG.warn( "\n\n" + SEPARATOR + "\n" + arrayFormat( message, args ).getMessage() + "\n" + SEPARATOR + "\n" );
    }

    public static void printEmphasizedErrorMessage( String message, Object... args )
    {
        LOG.error( "\n\n" + SEPARATOR + "\n" + arrayFormat( message, args ).getMessage() + "\n" + SEPARATOR + "\n" );
    }

    private EventsLogger()
    {
    }
}
