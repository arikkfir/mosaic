package org.mosaic.web.request;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.net.HttpStatus;

/**
 * @author arik
 */
public interface WebResponse
{
    boolean isCommitted();

    @Nullable
    HttpStatus getStatus();

    void setStatus( @Nonnull HttpStatus status, @Nullable String text );

    @Nonnull
    ByteBuffer getBytesContents();

    @Nonnull
    CharBuffer getCharsContents();

    @Nonnull
    WebResponseHeaders getHeaders();
}
