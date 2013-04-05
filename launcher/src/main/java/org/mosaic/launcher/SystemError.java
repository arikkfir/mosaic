package org.mosaic.launcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class SystemError
{
    private static final boolean showStackTraces = Boolean.getBoolean( "showErrors" ) || "".equals( System.getProperty( "showErrors" ) );

    @Nonnull
    public static BootstrapException bootstrapError( @Nonnull String message, @Nullable Object... args )
    {
        return new BootstrapException( String.format( message, args ) );
    }

    @Nonnull
    public static BootstrapException bootstrapError( @Nonnull String message,
                                                     @Nonnull Throwable cause,
                                                     @Nullable Object... args )
    {
        return new BootstrapException( String.format( message, args ), cause );
    }

    public static void handle( @Nonnull Throwable e )
    {
        System.err.printf( "%s\n", e.getMessage() );
        if( showStackTraces )
        {
            e.printStackTrace();
        }
    }

    private static class BootstrapException extends RuntimeException
    {
        private BootstrapException( @Nonnull String message )
        {
            super( message );
        }

        private BootstrapException( @Nonnull String message, @Nonnull Throwable cause )
        {
            super( message, cause );
        }
    }
}