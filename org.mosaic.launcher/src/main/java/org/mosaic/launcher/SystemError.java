package org.mosaic.launcher;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author arik
 */
final class SystemError
{
    public static BootstrapException bootstrapError( String message, Object... args )
    {
        return new BootstrapException( message, args );
    }

    public static class BootstrapException extends RuntimeException
    {
        private final String message;

        private BootstrapException( String message, Object[] arguments )
        {
            FormattingTuple tuple = MessageFormatter.arrayFormat( message, arguments );
            this.message = tuple.getMessage();
            initCause( tuple.getThrowable() );
        }

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
