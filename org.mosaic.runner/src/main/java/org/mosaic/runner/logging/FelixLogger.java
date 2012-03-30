package org.mosaic.runner.logging;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class FelixLogger extends Logger {

    private static final ThreadLocal<StringBuilder> BUFFERS = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder( 1000 );
        }
    };

    @Override
    protected void doLog( Bundle bundle, ServiceReference sr, int level, String msg, Throwable throwable ) {
        StringBuilder buf = BUFFERS.get();
        buf.delete( 0, buf.length() );
        String loggerName = "org.apache.felix";

        if( sr != null ) {
            buf.append( "ServiceReference: " ).append( sr ).append( " " );
            loggerName = sr.getBundle().getSymbolicName();

        } else if( bundle != null ) {
            buf.append( "Bundle: " ).append( bundle ).append( " " );
            loggerName = bundle.getSymbolicName();
        }
        buf.append( msg );
        if( throwable != null ) {
            buf.append( "(" ).append( throwable ).append( ")" );
        }

        org.slf4j.Logger logger = LoggerFactory.getLogger( loggerName );
        switch( level ) {
            case LOG_DEBUG:
                if( throwable != null ) {
                    logger.debug( buf.toString(), throwable );
                } else {
                    logger.debug( buf.toString() );
                }
                break;
            case LOG_ERROR:
                if( throwable != null ) {
                    logger.error( buf.toString(), throwable );
                } else {
                    logger.error( buf.toString() );
                }
                break;
            case LOG_INFO:
                if( throwable != null ) {
                    logger.info( buf.toString(), throwable );
                } else {
                    logger.info( buf.toString() );
                }
                break;
            case LOG_WARNING:
            default:
                if( throwable != null ) {
                    logger.warn( buf.toString(), throwable );
                } else {
                    logger.warn( buf.toString() );
                }
        }
    }
}
