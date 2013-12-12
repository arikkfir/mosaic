package org.mosaic.web.request;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;

/**
 * @author arik
 */
public interface WebResponse
{
    boolean isCommitted();

    @Nullable
    HttpStatus getStatus();

    void setStatus( @Nonnull HttpStatus status );

    void setStatus( @Nonnull HttpStatus status, @Nullable String text );

    void reset();

    @Nonnull
    OutputStream stream() throws IOException;

    @Nonnull
    Writer writer() throws IOException;

    void addCookie( @Nonnull String name, @Nonnull Object value );

    void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain );

    void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain, @Nonnull String path );

    void addCookie( @Nonnull String name,
                    @Nonnull Object value,
                    @Nonnull String domain,
                    @Nonnull String path,
                    @Nonnull Period maxAge );

    void removeCookie( @Nonnull String name );

    void disableCaching();
}
