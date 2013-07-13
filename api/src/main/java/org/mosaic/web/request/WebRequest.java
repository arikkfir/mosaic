package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.mosaic.security.User;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.net.MediaType;

/**
 * @author arik
 */
public interface WebRequest
{
    @Nonnull
    MapEx<String, Object> getAttributes();

    @Nonnull
    WebApplication getApplication();

    @Nonnull
    String getClientAddress();

    @Nonnull
    WebDevice getDevice();

    @Nonnull
    String getProtocol();

    @Nonnull
    HttpMethod getMethod();

    @Nonnull
    Uri getUri();

    @Nonnull
    Headers getHeaders();

    @Nonnull
    Body getBody();

    @Nonnull
    User getUser();

    @Nullable
    WebSession getSession();

    @Nonnull
    WebSession getOrCreateSession();

    @Nonnull
    WebResponse getResponse();

    void dumpToTraceLog( @Nullable String message, @Nullable Object... arguments );

    void dumpToDebugLog( @Nullable String message, @Nullable Object... arguments );

    void dumpToInfoLog( @Nullable String message, @Nullable Object... arguments );

    void dumpToWarnLog( @Nullable String message, @Nullable Object... arguments );

    void dumpToErrorLog( @Nullable String message, @Nullable Object... arguments );

    interface Uri
    {
        @Nonnull
        String getScheme();

        int getPort();

        @Nonnull
        String getHost();

        @Nonnull
        String getDecodedPath();

        @Nonnull
        String getEncodedPath();

        @Nullable
        MapEx<String, String> getPathParameters( @Nonnull String pathTemplate );

        @Nonnull
        Multimap<String, String> getDecodedQueryParameters();

        @Nonnull
        String getEncodedQueryString();

        @Nonnull
        String getFragment();
    }

    interface Headers
    {
        @Nonnull
        List<MediaType> getAccept();

        @Nonnull
        List<Charset> getAccepCharset();

        @Nonnull
        List<Locale> getAcceptLanguage();

        @Nullable
        String getAuthorization();

        @Nullable
        String getCacheControl();

        @Nullable
        String getConnection();

        @Nullable
        Long getContentLength();

        @Nullable
        MediaType getContentType();

        @Nullable
        RequestCookie getCookie( String name );

        @Nullable
        DateTime getDate();

        @Nullable
        String getExpect();

        @Nullable
        String getFrom();

        @Nonnull
        String getHost();

        @Nonnull
        Set<String> getIfMatch();

        @Nullable
        DateTime getIfModifiedSince();

        @Nonnull
        Set<String> getIfNoneMatch();

        @Nullable
        DateTime getIfUnmodifiedSince();

        @Nullable
        String getPragma();

        @Nullable
        String getReferer();

        @Nullable
        String getUserAgent();

        @Nonnull
        Multimap<String, String> getAllHeaders();
    }

    interface Body
    {
        @Nonnull
        InputStream asStream() throws IOException;

        @Nonnull
        Reader asReader() throws IOException;

        @Nullable
        WebPart getPart( @Nonnull String name ) throws IOException;

        @Nonnull
        Map<String, WebPart> getPartsMap() throws IOException;
    }
}
