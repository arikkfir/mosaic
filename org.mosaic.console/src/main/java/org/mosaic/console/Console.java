package org.mosaic.console;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Console
{
    int getWidth();

    int getHeight();

    char restrictedRead( char... allowed ) throws IOException;

    char restrictedPrompt( @Nonnull String prompt, char... allowed ) throws IOException;

    @Nullable
    String readLine() throws IOException;

    @Nonnull
    Console print( @Nullable Object value, @Nullable Object... args ) throws IOException;

    @Nonnull
    Console println() throws IOException;

    @Nonnull
    Console println( @Nullable Object value, @Nullable Object... args ) throws IOException;

    @Nonnull
    Console printStackTrace( @Nonnull Throwable throwable ) throws IOException;
}
