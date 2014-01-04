package org.mosaic.web.request.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Response;
import org.joda.time.DateTime;
import org.mosaic.web.http.HttpResponse;
import org.mosaic.web.http.HttpStatus;

import static com.google.common.net.HttpHeaders.*;

/**
 * @author arik
 */
final class JettyHttpResponseImpl implements HttpResponse
{
    @Nonnull
    private final Response response;

    @Nonnull
    private final Headers headers;

    JettyHttpResponseImpl( @Nonnull Response response )
    {
        this.response = response;
        this.headers = new Headers( this.response.getHttpFields() );
    }

    @Nonnull
    public Response getJettyResponse()
    {
        return this.response;
    }

    @Nullable
    @Override
    public HttpStatus getStatus()
    {
        return HttpStatus.valueOf( this.response.getStatus() );
    }

    @Override
    public void setStatus( @Nonnull HttpStatus status, @Nullable String text )
    {
        this.response.setStatusWithReason( status.value(), text );
    }

    @Nonnull
    @Override
    public List<String> getAcceptRanges()
    {
        return this.headers.getStrings( ACCEPT_RANGES );
    }

    @Override
    public void setAcceptRanges( @Nullable List<String> values )
    {
        this.headers.setStrings( ACCEPT_RANGES, values );
    }

    @Nullable
    @Override
    public List<String> getAllow()
    {
        return this.headers.getStrings( ALLOW );
    }

    @Override
    public void setAllow( @Nullable List<String> value )
    {
        this.headers.setStrings( ALLOW, value );
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return this.headers.getString( CACHE_CONTROL );
    }

    @Override
    public void setCacheControl( @Nullable String value )
    {
        this.headers.setString( CACHE_CONTROL, value );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return this.headers.getString( CONNECTION );
    }

    @Override
    public void setConnection( @Nullable String value )
    {
        this.headers.setString( CONNECTION, value );
    }

    @Nonnull
    @Override
    public List<String> getContentEncoding()
    {
        return this.headers.getStrings( CONTENT_ENCODING );
    }

    @Override
    public void setContentEncoding( @Nullable List<String> value )
    {
        this.headers.setStrings( CONTENT_ENCODING, value );
    }

    @Nullable
    @Override
    public List<Locale> getContentLanguage()
    {
        return this.headers.getLocales( CONTENT_LANGUAGE );
    }

    @Override
    public void setContentLanguage( @Nullable List<Locale> value )
    {
        this.headers.setLocales( CONTENT_LANGUAGE, value );
    }

    @Nullable
    @Override
    public Long getContentLength()
    {
        long contentLength = this.response.getLongContentLength();
        return contentLength < 0 ? null : contentLength;
    }

    @Override
    public void setContentLength( @Nullable Long value )
    {
        this.response.setLongContentLength( value == null ? -1 : value );
    }

    @Nullable
    @Override
    public String getContentLocation()
    {
        return this.headers.getString( CONTENT_LOCATION );
    }

    @Override
    public void setContentLocation( @Nullable String value )
    {
        this.headers.setString( CONTENT_LOCATION, value );
    }

    @Nullable
    @Override
    public String getContentMd5()
    {
        return this.headers.getString( CONTENT_MD5 );
    }

    @Override
    public void setContentMd5( @Nullable String value )
    {
        this.headers.setString( CONTENT_MD5, value );
    }

    @Nullable
    @Override
    public String getContentRange()
    {
        return this.headers.getString( CONTENT_RANGE );
    }

    @Override
    public void setContentRange( @Nullable String value )
    {
        this.headers.setString( CONTENT_RANGE, value );
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        return MediaType.parse( this.response.getContentType() );
    }

    @Override
    public void setContentType( @Nullable MediaType value )
    {
        this.response.setContentType( Objects.toString( value, null ) );
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        return this.headers.getDateTime( DATE );
    }

    @Override
    public void setDate( @Nullable DateTime value )
    {
        this.headers.setDateTime( DATE, value );
    }

    @Nullable
    @Override
    public String getETag()
    {
        return this.headers.getString( ETAG );
    }

    @Override
    public void setETag( @Nullable String value )
    {
        this.headers.setString( ETAG, value );
    }

    @Nullable
    @Override
    public DateTime getExpires()
    {
        return this.headers.getDateTime( EXPIRES );
    }

    @Override
    public void setExpires( @Nullable DateTime value )
    {
        this.headers.setDateTime( EXPIRES, value );
    }

    @Nullable
    @Override
    public DateTime getLastModified()
    {
        return this.headers.getDateTime( LAST_MODIFIED );
    }

    @Override
    public void setLastModified( @Nullable DateTime value )
    {
        this.headers.setDateTime( LAST_MODIFIED, value );
    }

    @Nullable
    @Override
    public String getLocation()
    {
        return this.headers.getString( LOCATION );
    }

    @Override
    public void setLocation( @Nullable String value ) throws IOException
    {
        this.headers.setString( LOCATION, value );
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return this.headers.getString( PRAGMA );
    }

    @Override
    public void setPragma( @Nullable String value )
    {
        this.headers.setString( PRAGMA, value );
    }

    @Nullable
    @Override
    public Integer getRetryAfter()
    {
        return this.headers.getInteger( RETRY_AFTER );
    }

    @Override
    public void setRetryAfter( @Nullable Integer value )
    {
        this.headers.setInteger( RETRY_AFTER, value );
    }

    @Nullable
    @Override
    public String getServer()
    {
        return this.headers.getString( SERVER );
    }

    @Override
    public void setServer( @Nullable String value )
    {
        this.headers.setString( SERVER, value );
    }

    @Nonnull
    @Override
    public Collection<String> getVary()
    {
        return this.headers.getStrings( VARY );
    }

    @Override
    public void setVary( @Nullable List<String> value )
    {
        this.headers.setStrings( VARY, value );
    }

    @Nullable
    @Override
    public String getVia()
    {
        return this.headers.getString( VIA );
    }

    @Override
    public void setVia( @Nullable String value )
    {
        this.headers.setString( VIA, value );
    }

    @Nullable
    @Override
    public String getWarning()
    {
        return this.headers.getString( WARNING );
    }

    @Override
    public void setWarning( @Nullable String value )
    {
        this.headers.setString( WARNING, value );
    }

    @Nullable
    @Override
    public String getWwwAuthenticate()
    {
        return this.headers.getString( WWW_AUTHENTICATE );
    }

    @Override
    public void setWwwAuthenticate( @Nullable String value )
    {
        this.headers.setString( WWW_AUTHENTICATE, value );
    }

    @Override
    public int getHeadersCount()
    {
        return this.headers.size();
    }

    @Nonnull
    @Override
    public Set<String> getHeaderNames()
    {
        return this.headers.keySet();
    }

    @Override
    public boolean containsHeader( @Nonnull String key )
    {
        return this.headers.containsKey( key );
    }

    @Override
    public boolean containsHeader( @Nonnull String key, @Nonnull String value )
    {
        return this.headers.containsEntry( key, value );
    }

    @Nonnull
    @Override
    public List<String> getHeader( @Nonnull String key )
    {
        return this.headers.getStrings( key );
    }

    @Override
    public void setHeader( @Nonnull String key, @Nonnull String... values )
    {
        this.headers.setStrings( key, Arrays.asList( values ) );
    }

    @Override
    public void setHeader( @Nonnull String key, @Nonnull List<String> values )
    {
        this.headers.setStrings( key, values );
    }

    @Nonnull
    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Nonnull
    @Override
    public Writer getWriter() throws IOException
    {
        return this.response.getWriter();
    }

    @Override
    public <T> void marshall( @Nonnull T value )
    {
        // TODO arik: implement org.mosaic.web.http.impl.HttpResponseImpl.marshall([value])
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    @Override
    public void reset()
    {
        this.response.reset( false );
    }
}
