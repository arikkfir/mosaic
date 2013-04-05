package org.mosaic.launcher.logging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public abstract class EventsLogger
{
    public static void printEmphasizedInfoMessage( @Nonnull String message, @Nullable Object... args )
    {
        LoggerFactory.getLogger( "org.osgi.framework" ).info( "" );
        LoggerFactory.getLogger( "org.osgi.framework" ).info( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).info( message, args );
        LoggerFactory.getLogger( "org.osgi.framework" ).info( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).info( "" );
    }

    public static void printEmphasizedWarnMessage( @Nonnull String message, @Nullable Object... args )
    {
        LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );
        LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).warn( message, args );
        LoggerFactory.getLogger( "org.osgi.framework" ).warn( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).warn( "" );
    }

    public static void printEmphasizedErrorMessage( @Nonnull String message, @Nullable Object... args )
    {
        LoggerFactory.getLogger( "org.osgi.framework" ).error( "" );
        LoggerFactory.getLogger( "org.osgi.framework" ).error( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).error( message, args );
        LoggerFactory.getLogger( "org.osgi.framework" ).error( "*****************************************************************************************" );
        LoggerFactory.getLogger( "org.osgi.framework" ).error( "" );
    }
}
