package org.mosaic.web.server;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

/**
 * @author arik
 */
public interface HttpResponse
{
    @Nullable
    HttpStatus getStatus();

    void setStatus( @Nonnull HttpStatus status, @Nullable String text );

    @Nonnull
    List<String> getAcceptRanges();

    void setAcceptRanges( @Nullable List<String> values );

    @Nullable
    List<String> getAllow();

    void setAllow( @Nullable List<String> value );

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
    List<Locale> getContentLanguage();

    void setContentLanguage( @Nullable List<Locale> value );

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

    @Nonnull
    Collection<String> getVary();

    void setVary( @Nullable List<String> value );

    @Nullable
    String getVia();

    void setVia( @Nullable String value );

    @Nullable
    String getWarning();

    void setWarning( @Nullable String value );

    @Nullable
    String getWwwAuthenticate();

    void setWwwAuthenticate( @Nullable String value );

    int getHeadersCount();

    @Nonnull
    Set<String> getHeaderNames();

    boolean containsHeader( @Nonnull String key );

    boolean containsHeader( @Nonnull String key, @Nonnull String value );

    @Nonnull
    List<String> getHeader( @Nonnull String key );

    void setHeader( @Nonnull String key, @Nonnull String... values );

    void setHeader( @Nonnull String key, @Nonnull List<String> values );

    @Nonnull
    OutputStream getOutputStream() throws IOException;

    @Nonnull
    Writer getWriter() throws IOException;

    <T> void marshall( @Nonnull T value );

    boolean isCommitted();

    void reset();
}
