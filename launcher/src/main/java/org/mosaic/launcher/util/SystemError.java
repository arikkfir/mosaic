package org.mosaic.launcher.util;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author arik
 */
public class SystemError
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemError.class );

    @Nonnull
    public static BootstrapException bootstrapError( @Nonnull String message, @Nonnull Object... args )
    {
        return new BootstrapException( message, args );
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
        private final String message;

        @Nonnull
        private final Object[] arguments;

        private BootstrapException( @Nonnull String message, @Nonnull Object[] arguments )
        {
            FormattingTuple tuple = MessageFormatter.arrayFormat( message, arguments );
            this.message = tuple.getMessage();
            this.arguments = tuple.getArgArray();
            initCause( tuple.getThrowable() );
        }

        @Nonnull
        @Override
        public String getMessage()
        {
            return this.message;
        }

        @Override
        public String getLocalizedMessage()
        {
            return this.message;
        }

        @Nonnull
        public Object[] getArguments()
        {
            return arguments;
        }
    }
}
