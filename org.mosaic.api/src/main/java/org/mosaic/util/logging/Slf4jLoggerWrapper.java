package org.mosaic.util.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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

    @Override
    public String getName( ) {
        return this.logger.getName( );
    }

    @Override
    public boolean isTraceEnabled( ) {
        return logger.isTraceEnabled( );
    }

    @Override
    public Logger trace( String msg, Object... args ) {
        if( isTraceEnabled( ) ) {
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
                this.logger.trace( tuple.getMessage( ), throwable );
            } else {
                this.logger.trace( tuple.getMessage( ) );
            }
        }
        return this;
    }

    @Override
    public boolean isDebugEnabled( ) {
        return logger.isDebugEnabled( );
    }

    @Override
    public Logger debug( String msg, Object... args ) {
        if( isDebugEnabled( ) ) {
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

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage( );
            if( throwable != null ) {
                this.logger.debug( msg, throwable );
            } else {
                this.logger.debug( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isInfoEnabled( ) {
        return logger.isInfoEnabled( );
    }

    @Override
    public Logger info( String msg, Object... args ) {
        if( isInfoEnabled( ) ) {
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

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage( );
            if( throwable != null ) {
                this.logger.info( msg, throwable );
            } else {
                this.logger.info( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isWarnEnabled( ) {
        return logger.isWarnEnabled( );
    }

    @Override
    public Logger warn( String msg, Object... args ) {
        if( isWarnEnabled( ) ) {
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

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage( );
            if( throwable != null ) {
                this.logger.warn( msg, throwable );
            } else {
                this.logger.warn( msg );
            }
        }
        return this;
    }

    @Override
    public boolean isErrorEnabled( ) {
        return logger.isErrorEnabled( );
    }

    @Override
    public Logger error( String msg, Object... args ) {
        if( isErrorEnabled( ) ) {
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

            msg = MessageFormatter.arrayFormat( msg, args ).getMessage( );
            if( throwable != null ) {
                this.logger.error( msg, throwable );
            } else {
                this.logger.error( msg );
            }
        }

        return this;
    }

    @Override
    public PrintWriter getPrintWriter( ) {
        return new PrintWriter( new LoggerWriter( ) );
    }

    private class LoggerWriter extends Writer {

        private final StringBuilder buffer = new StringBuilder( 4096 );

        @Override
        public void write( char[] chars, int off, int len ) throws IOException {
            synchronized( this ) {
                for( int i = off; i < chars.length && i < off + len; i++ ) {
                    char c = chars[ i ];
                    if( c == '\n' ) {
                        info( this.buffer.toString( ) );
                        this.buffer.delete( 0, Integer.MAX_VALUE );
                    } else {
                        this.buffer.append( c );
                    }
                }
            }
        }

        @Override
        public void flush( ) throws IOException {
            // no-op
        }

        @Override
        public void close( ) throws IOException {
            // no-op
        }
    }
}
