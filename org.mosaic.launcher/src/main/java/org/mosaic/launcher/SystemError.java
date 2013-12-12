package org.mosaic.launcher;

import javax.annotation.Nonnull;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author arik
 */
final class SystemError
{
    @Nonnull
    public static BootstrapException bootstrapError( @Nonnull String message, @Nonnull Object... args )
    {
        return new BootstrapException( message, args );
    }

    public static class BootstrapException extends RuntimeException
    {
        @Nonnull
        private final String message;

        private BootstrapException( @Nonnull String message, @Nonnull Object[] arguments )
        {
            FormattingTuple tuple = MessageFormatter.arrayFormat( message, arguments );
            this.message = tuple.getMessage();
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
    }

    private SystemError()
    {
    }
}
