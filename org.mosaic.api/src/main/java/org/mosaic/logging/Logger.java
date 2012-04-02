package org.mosaic.logging;

/**
 * @author arik
 */
public interface Logger {

    String getName();

    boolean isTraceEnabled();

    Logger trace( String msg, Object... args );

    boolean isDebugEnabled();

    Logger debug( String msg, Object... args );

    boolean isInfoEnabled();

    Logger info( String msg, Object... args );

    boolean isWarnEnabled();

    Logger warn( String msg, Object... args );

    boolean isErrorEnabled();

    Logger error( String msg, Object... args );

}
