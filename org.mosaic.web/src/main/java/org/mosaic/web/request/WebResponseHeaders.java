package org.mosaic.web.request;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

/**
 * @author arik
 */
public interface WebResponseHeaders
{
    @Nullable
    List<String> getAllow();

    void setAllow( @Nullable List<String> value );

    @Nonnull
    List<String> getAcceptRanges();

    void setAcceptRanges( @Nullable List<String> values );

    @Nullable
    String getCacheControl();

    void setCacheControl( @Nullable String value );

    @Nullable
    String getConnection();

    void setConnection( @Nullable String value );

    @Nonnull
    List<String> getContentEncoding();

    void setContentEncoding( @Nullable List<String> value );

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
    String getContentMd5();

    void setContentMd5( @Nullable String value );

    @Nullable
    String getContentRange();

    void setContentRange( @Nullable String value );

    @Nullable
    MediaType getContentType();

    void setContentType( @Nullable MediaType value );

    @Nullable
    DateTime getExpires();

    void setExpires( @Nullable DateTime value );

    @Nullable
    DateTime getDate();

    void setDate( @Nullable DateTime value );

    @Nullable
    String getETag();

    void setETag( @Nullable String value );

    @Nullable
    String getLocation();

    void setLocation( @Nullable String value ) throws IOException;

    @Nullable
    DateTime getLastModified();

    void setLastModified( @Nullable DateTime value );

    @Nullable
    String getPragma();

    void setPragma( @Nullable String value );

    @Nullable
    Integer getRetryAfter();

    void setRetryAfter( @Nullable Integer value );

    @Nullable
    String getServer();

    void setServer( @Nullable String value );

    @Nonnull
    Collection<String> getVary();

    void setVary( @Nullable List<String> value );

    @Nullable
    String getWarning();

    void setWarning( @Nullable String value );

    @Nullable
    String getWwwAuthenticate();

    void setWwwAuthenticate( @Nullable String value );

    void addHeader( @Nonnull String name, @Nonnull String value );

    void addHeader( @Nonnull String name, @Nonnull DateTime value );

    void addHeader( @Nonnull String name, @Nonnull Integer value );

    void addHeader( @Nonnull String name, @Nonnull Long value );

    void setHeader( @Nonnull String name, @Nonnull String value );

    void setHeader( @Nonnull String name, @Nonnull DateTime value );

    void setHeader( @Nonnull String name, @Nonnull Integer value );

    void setHeader( @Nonnull String name, @Nonnull Long value );

    void removeHeader( @Nonnull String name );
}
