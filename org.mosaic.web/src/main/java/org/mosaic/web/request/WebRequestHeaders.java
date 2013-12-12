package org.mosaic.web.request;

import com.google.common.net.MediaType;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

/**
 * @author arik
 */
public interface WebRequestHeaders
{
    @Nonnull
    List<MediaType> getAccept();

    @Nonnull
    List<Charset> getAccepCharset();

    @Nonnull
    List<String> getAcceptEncoding();

    @Nonnull
    List<Locale> getAcceptLanguage();

    @Nullable
    String getAuthorization();

    @Nullable
    String getCacheControl();

    @Nullable
    String getConnection();

    @Nonnull
    Collection<String> getContentEncoding();

    @Nonnull
    Locale getContentLanguage();

    @Nonnull
    List<Locale> getContentLanguages();

    @Nullable
    Integer getContentLength();

    @Nullable
    URI getContentLocation();

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
    Collection<String> getIfMatch();

    @Nullable
    DateTime getIfModifiedSince();

    @Nonnull
    Collection<String> getIfNoneMatch();

    @Nullable
    DateTime getIfRangeDate();

    @Nullable
    String getIfRangeETag();

    @Nullable
    DateTime getIfUnmodifiedSince();

    @Nullable
    String getPragma();

    @Nonnull
    Collection<String> getRange();

    @Nullable
    String getReferer();

    @Nullable
    String getTe();

    @Nullable
    String getVia();

    @Nullable
    String getUserAgent();

    @Nullable
    String getWarning();

    int size();

    boolean isEmpty();

    boolean containsKey( @Nonnull String key );

    Collection<String> getValues( @Nonnull String key, boolean splitValues, boolean supportsQuality );
}
