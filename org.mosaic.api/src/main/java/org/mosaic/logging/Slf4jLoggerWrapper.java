package org.mosaic.logging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author arik
 */
public class Slf4jLoggerWrapper implements Logger {

    private final org.slf4j.Logger logger;

    public Slf4jLoggerWrapper( org.slf4j.Logger logger ) {
        if( logger == null ) {
            throw new NullPointerException( "Logger must not be null" );
        }
        this.logger = logger;
    }

    @Nonnull
    @Override
    public String getName() {
        return this.logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Nonnull
    @Override
    public Logger trace( @Nullable String msg, @Nullable Object... args ) {
        if( isTraceEnabled() ) {
            if( msg == null ) {
                msg = "";
            }

            Throwable throwable = null;
            if( args != null && args.length > 0 && args[ args.length - 1 ] instanceof Throwable ) {
                throwable = ( Throwable ) args[ args.length - 1 ];
                Object[] newArgs = new Object[ args.length - 1 ];
                System.arraycopy( args, 0, newArgs, 0, args.length - 1 );
                args = newArgs;
            }

            FormattingTuple tuple = MessageFormatter.arrayFormat( msg, args );
            if( throwable != null ) {
                this.logger.trace( tuple.getMessage(), throwable );
            } else {
                this.logger.trace( tuple.getMessage() );
            }
        }
        return this;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Nonnull
    @Override
    public Logger debug( @Nullable String msg, @Nullable Object... args ) {
        if( isDebugEnabled() ) {
            if( msg == null ) {
                msg = "";
            }

            Throwable throwable = null;
            if( args != null && args.length > 0 && args[ args.length - 1 ] instanceof Throwable ) {
                throwable = ( Throwable ) args[ args.length - 1 ];
                Object[] newArgs = new Object[ args.length - 1 ];
                System.arraycopy( args, 0, newArgs, 0, args.length - 1 );
                args = newArgs;
            }

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage();
            if( throwable != null ) {
                this.logger.debug( msg, throwable );
            } else {
                this.logger.debug( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Nonnull
    @Override
    public Logger info( @Nullable String msg, @Nullable Object... args ) {
        if( isInfoEnabled() ) {
            if( msg == null ) {
                msg = "";
            }

            Throwable throwable = null;
            if( args != null && args.length > 0 && args[ args.length - 1 ] instanceof Throwable ) {
                throwable = ( Throwable ) args[ args.length - 1 ];
                Object[] newArgs = new Object[ args.length - 1 ];
                System.arraycopy( args, 0, newArgs, 0, args.length - 1 );
                args = newArgs;
            }

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage();
            if( throwable != null ) {
                this.logger.info( msg, throwable );
            } else {
                this.logger.info( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Nonnull
    @Override
    public Logger warn( @Nullable String msg, @Nullable Object... args ) {
        if( isWarnEnabled() ) {
            if( msg == null ) {
                msg = "";
            }

            Throwable throwable = null;
            if( args != null && args.length > 0 && args[ args.length - 1 ] instanceof Throwable ) {
                throwable = ( Throwable ) args[ args.length - 1 ];
                Object[] newArgs = new Object[ args.length - 1 ];
                System.arraycopy( args, 0, newArgs, 0, args.length - 1 );
                args = newArgs;
            }

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage();
            if( throwable != null ) {
                this.logger.warn( msg, throwable );
            } else {
                this.logger.warn( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Nonnull
    @Override
    public Logger error( @Nullable String msg, @Nullable Object... args ) {
        if( isErrorEnabled() ) {
            if( msg == null ) {
                msg = "";
            }

            Throwable throwable = null;
            if( args != null && args.length > 0 && args[ args.length - 1 ] instanceof Throwable ) {
                throwable = ( Throwable ) args[ args.length - 1 ];
                Object[] newArgs = new Object[ args.length - 1 ];
                System.arraycopy( args, 0, newArgs, 0, args.length - 1 );
                args = newArgs;
            }

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage();
            if( throwable != null ) {
                this.logger.error( msg, throwable );
            } else {
                this.logger.error( msg );
            }
        }

        return this;
    }
}
