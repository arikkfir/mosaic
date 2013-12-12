package org.mosaic.launcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class EventsLogger
{
    private static final Logger LOG = LoggerFactory.getLogger( "org.mosaic.events" );

    private static final String SEPARATOR = "******************************************************************************";

    @SuppressWarnings( "UnusedDeclaration" )
    public static void printEmphasizedInfoMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.info( "" );
        LOG.info( SEPARATOR );
        LOG.info( message, args );
        LOG.info( SEPARATOR );
        LOG.info( "" );
    }

    public static void printEmphasizedWarnMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.warn( "" );
        LOG.warn( SEPARATOR );
        LOG.warn( message, args );
        LOG.warn( SEPARATOR );
        LOG.warn( "" );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static void printEmphasizedErrorMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.error( "" );
        LOG.warn( SEPARATOR );
        LOG.error( message, args );
        LOG.warn( SEPARATOR );
        LOG.error( "" );
    }

    private EventsLogger()
    {
    }
}
