package org.mosaic.web.http;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

/**
 * @author arik
 */
public interface HttpRequest
{
    @Nonnull
    String getClientAddress();

    @Nonnull
    String getMethod();

    @Nonnull
    HttpRequestUri getUri();

    @Nonnull
    String getProtocol();

    @Nonnull
    List<MediaType> getAccept();

    @Nonnull
    List<Charset> getAccepCharset();

    @Nonnull
    List<String> getAcceptEncoding();

    @Nonnull
    List<Locale> getAcceptLanguage();

    @Nonnull
    List<String> getAllow();

    @Nullable
    String getAuthorization();

    @Nullable
    String getCacheControl();

    @Nullable
    String getConnection();

    @Nonnull
    List<String> getContentEncoding();

    @Nullable
    Locale getContentLanguage();

    @Nonnull
    List<Locale> getContentLanguages();

    @Nullable
    Long getContentLength();

    @Nullable
    String getContentLocation();

    @Nullable
    String getContentMd5();

    @Nullable
    String getContentRange();

    @Nullable
    MediaType getContentType();

    @Nullable
    DateTime getDate();

    @Nullable
    String getExpect();

    @Nullable
    String getFrom();

    @Nonnull
    String getHost();

    @Nonnull
    List<String> getIfMatch();

    @Nullable
    DateTime getIfModifiedSince();

    @Nonnull
    List<String> getIfNoneMatch();

    @Nullable
    DateTime getIfRangeDate();

    @Nullable
    String getIfRangeETag();

    @Nullable
    DateTime getIfUnmodifiedSince();

    @Nullable
    String getPragma();

    @Nonnull
    List<String> getRange();

    @Nullable
    String getReferer();

    @Nullable
    String getUserAgent();

    @Nullable
    String getVia();

    @Nullable
    String getWarning();

    int getHeadersCount();

    @Nonnull
    Set<String> getHeaderNames();

    boolean containsHeader( @Nonnull String key );

    boolean containsHeader( @Nonnull String key, @Nonnull String value );

    @Nonnull
    List<String> getHeader( @Nonnull String key );

    @Nonnull
    InputStream stream() throws IOException;

    @Nonnull
    Reader reader() throws IOException;

    @Nonnull
    <T> T unmarshall( @Nonnull Class<T> type );

    @Nullable
    HttpRequestPart getPart( @Nonnull String name ) throws IOException;

    @Nonnull
    Map<String, ? extends HttpRequestPart> getParts() throws IOException;

    interface HttpRequestPart
    {
        @Nonnull
        String getName();

        @Nullable
        Long getContentLength();

        @Nullable
        MediaType getContentType();

        @Nonnull
        InputStream stream() throws IOException;

        @Nonnull
        Reader reader() throws IOException;

        @Nonnull
        <T> T unmarshall( @Nonnull Class<T> type );

        void saveLocally( @Nonnull Path file ) throws IOException;

        @Nonnull
        Path saveToTempFile() throws IOException;
    }
}
