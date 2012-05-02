package org.mosaic.util.logging;

import java.io.PrintWriter;

/**
 * @author arik
 */
public interface Logger {

    String getName( );

    boolean isTraceEnabled( );

    @SuppressWarnings( "UnusedDeclaration" )
    Logger trace( String msg, Object... args );

    boolean isDebugEnabled( );

    Logger debug( String msg, Object... args );

    boolean isInfoEnabled( );

    Logger info( String msg, Object... args );

    boolean isWarnEnabled( );

    Logger warn( String msg, Object... args );

    boolean isErrorEnabled( );

    Logger error( String msg, Object... args );

    PrintWriter getPrintWriter( );
}
