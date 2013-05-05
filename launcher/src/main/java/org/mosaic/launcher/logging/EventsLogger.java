package org.mosaic.launcher.logging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public abstract class EventsLogger
{
    private static final Logger LOG = LoggerFactory.getLogger( "org.osgi.framework" );

    public static void printEmphasizedInfoMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.info( "" );
        LOG.info( "*****************************************************************************************" );
        LOG.info( message, args );
        LOG.info( "*****************************************************************************************" );
        LOG.info( "" );
    }

    public static void printEmphasizedWarnMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.warn( "" );
        LOG.warn( "*****************************************************************************************" );
        LOG.warn( message, args );
        LOG.warn( "*****************************************************************************************" );
        LOG.warn( "" );
    }

    public static void printEmphasizedErrorMessage( @Nonnull String message, @Nullable Object... args )
    {
        LOG.error( "" );
        LOG.error( "*****************************************************************************************" );
        LOG.error( message, args );
        LOG.error( "*****************************************************************************************" );
        LOG.error( "" );
    }
}
