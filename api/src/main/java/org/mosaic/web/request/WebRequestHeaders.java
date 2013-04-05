package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.net.MediaType;

/**
 * @author arik
 */
public interface WebRequestHeaders extends Multimap<String, String>
{
    @Nonnull
    MediaType getFirstAccept();

    @Nonnull
    List<MediaType> getAccept();

    @Nonnull
    Charset getFirstAcceptCharset();

    @Nonnull
    List<Charset> getAcceptCharset();

    @Nonnull
    Locale getFirstAcceptLanguage();

    @Nonnull
    List<Locale> getAcceptLanguage();

    @Nullable
    String getAuthorization();

    @Nullable
    String getCacheControl();

    @Nonnull
    List<Locale> getContentLanguage();

    @Nullable
    Long getContentLength();

    @Nullable
    MediaType getContentType();

    @Nullable
    WebCookie getCookie( String name );

    @Nonnull
    Set<String> getIfMatch();

    @Nullable
    Date getIfModifiedSince();

    @Nonnull
    Set<String> getIfNoneMatch();

    @Nullable
    Date getIfUnmodifiedSince();

    @Nullable
    String getPragma();

    @Nullable
    String getReferer();

    @Nullable
    String getUserAgent();
}
