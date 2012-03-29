package org.mosaic.logging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Logger {

    @Nonnull
    String getName();

    boolean isTraceEnabled();

    @Nonnull
    Logger trace( @Nullable String msg, @Nullable Object... args );

    boolean isDebugEnabled();

    @Nonnull
    Logger debug( @Nullable String msg, @Nullable Object... args );

    boolean isInfoEnabled();

    @Nonnull
    Logger info( @Nullable String msg, @Nullable Object... args );

    boolean isWarnEnabled();

    @Nonnull
    Logger warn( @Nullable String msg, @Nullable Object... args );

    boolean isErrorEnabled();

    @Nonnull
    Logger error( @Nullable String msg, @Nullable Object... args );

}
