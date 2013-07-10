package org.mosaic.web.request;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.net.MediaType;

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

    @Nonnull
    OutputStream getBinaryBody() throws IOException;

    @Nonnull
    Writer getCharacterBody() throws IOException;

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

    void allowPublicCaches( @Nonnull Period maxAge );

    void allowOnlyPrivateCaches( @Nonnull Period maxAge );

    @Nonnull
    Headers getHeaders();

    interface Headers
    {
        @Nullable
        Collection<HttpMethod> getAllow();

        void setAllow( @Nullable Collection<HttpMethod> value );

        @Nullable
        String getCacheControl();

        void setCacheControl( @Nullable String value );

        @Nullable
        String getConnection();

        void setConnection( @Nullable String value );

        @Nullable
        Locale getContentLanguage();

        void setContentLanguage( @Nullable Locale value );

        @Nullable
        Long getContentLength();

        void setContentLength( @Nullable Long value );

        @Nullable
        String getContentLocation();

        void setContentLocation( @Nullable String value );

        @Nullable
        MediaType getContentType();

        void setContentType( @Nullable MediaType value );

        @Nullable
        DateTime getDate();

        void setDate( @Nullable DateTime value );

        @Nullable
        String getETag();

        void setETag( @Nullable String value );

        @Nullable
        DateTime getExpires();

        void setExpires( @Nullable DateTime value );

        @Nullable
        DateTime getLastModified();

        void setLastModified( @Nullable DateTime value );

        @Nullable
        String getLocation();

        void setLocation( @Nullable String value ) throws IOException;

        @Nullable
        String getPragma();

        void setPragma( @Nullable String value );

        @Nullable
        Integer getRetryAfter();

        void setRetryAfter( @Nullable Integer value );

        @Nullable
        String getServer();

        void setServer( @Nullable String value );

        @Nullable
        String getWwwAuthenticate();

        void setWwwAuthenticate( @Nullable String value );

        void setHeader( @Nonnull String name, @Nonnull String value );

        void setHeader( @Nonnull String name, @Nonnull Collection<String> values );
    }
}
