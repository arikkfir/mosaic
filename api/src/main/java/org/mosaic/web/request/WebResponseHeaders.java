package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.net.HttpMethod;
import org.mosaic.net.MediaType;

/**
 * @author arik
 */
public interface WebResponseHeaders extends Multimap<String, String>
{
    @Nonnull
    Set<HttpMethod> getAllow();

    void setAllow( @Nonnull Collection<HttpMethod> methods );

    @Nullable
    String getCacheControl();

    void setCacheControl( @Nullable String cacheControl );

    @Nullable
    Locale getContentLanguage();

    void setContentLanguage( @Nullable Locale contentLanguage );

    @Nullable
    Long getContentLength();

    void setContentLength( @Nullable Long contentLength );

    @Nullable
    MediaType getContentType();

    void setContentType( @Nullable MediaType contentType );

    @Nullable
    Charset getContentCharset();

    void setContentCharset( @Nullable Charset charset );

    @Nonnull
    WebCookie addOrUpdateCookie( @Nonnull String name, @Nullable Object value );

    @Nullable
    String getETag();

    void setETag( @Nullable String eTag );

    @Nullable
    Long getExpires();

    void setExpires( @Nullable Long seconds );

    @Nullable
    Date getLastModified();

    void setLastModified( @Nullable Date lastModified );

    @Nullable
    String getLocation();

    void setLocation( @Nullable String location );

    @Nullable
    String getPragma();

    void setPragma( @Nullable String pragma );

    @Nullable
    Integer getRetryAfter();

    void setRetryAfter( @Nullable Integer retryAfter );

    @Nullable
    String getWwwAuthenticate();

    void setWwwAuthenticate( @Nullable String wwwAuthenticate );

    void disableCache();
}
