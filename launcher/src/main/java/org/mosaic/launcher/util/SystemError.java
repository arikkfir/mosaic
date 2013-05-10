package org.mosaic.launcher.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class SystemError
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemError.class );

    @Nonnull
    public static BootstrapException bootstrapError( @Nonnull String message, @Nonnull Object... args )
    {
        return new BootstrapException( message, null, args );
    }

    public static void handle( @Nonnull Throwable e )
    {
        Object[] arguments;
        if( e instanceof BootstrapException )
        {
            BootstrapException be = ( BootstrapException ) e;
            arguments = be.arguments;
        }
        else
        {
            arguments = new Object[] { e };
        }
        LOG.error( e.getMessage(), arguments );
    }

    public static class BootstrapException extends RuntimeException
    {
        @Nonnull
        private final Object[] arguments;

        private BootstrapException( @Nonnull String message, @Nullable Throwable cause, @Nonnull Object[] arguments )
        {
            super( message, cause );
            this.arguments = arguments;
        }
    }
}
