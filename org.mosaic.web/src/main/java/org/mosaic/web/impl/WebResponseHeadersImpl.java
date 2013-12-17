package org.mosaic.web.impl;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.joda.time.DateTime;
import org.mosaic.web.request.WebResponseHeaders;

import static com.google.common.net.HttpHeaders.*;
import static org.mosaic.web.impl.HeaderUtil.*;

/**
 * @author arik
 */
final class WebResponseHeadersImpl implements WebResponseHeaders
{
    private static final Joiner HEADER_MULTIVALUE_JOINER = Joiner.on( ',' ).skipNulls();

    @Nonnull
    private final Request request;

    @Nonnull
    private final Response response;

    WebResponseHeadersImpl( @Nonnull Request request, @Nonnull Response response )
    {
        this.request = request;
        this.response = response;
    }

    @Nullable
    @Override
    public List<String> getAllow()
    {
        return HeaderUtil.getStrings( this.response.getHttpFields(), ALLOW );
    }

    @Override
    public void setAllow( @Nullable List<String> values )
    {
        this.response.setHeader( ALLOW, values == null ? null : HEADER_MULTIVALUE_JOINER.join( values ) );
    }

    @Nonnull
    @Override
    public List<String> getAcceptRanges()
    {
        return getStrings( this.response.getHttpFields(), ACCEPT_RANGES );
    }

    @Override
    public void setAcceptRanges( @Nullable List<String> values )
    {
        this.response.setHeader( ACCEPT_RANGES, values == null ? null : HEADER_MULTIVALUE_JOINER.join( values ) );
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return getString( this.response.getHttpFields(), CACHE_CONTROL );
    }

    @Override
    public void setCacheControl( @Nullable String value )
    {
        this.response.setHeader( CACHE_CONTROL, value );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return getString( this.response.getHttpFields(), CONNECTION );
    }

    @Override
    public void setConnection( @Nullable String value )
    {
        this.response.setHeader( CONNECTION, value );
    }

    @Nonnull
    @Override
    public List<String> getContentEncoding()
    {
        return getStrings( this.response.getHttpFields(), CONTENT_ENCODING );
    }

    @Override
    public void setContentEncoding( @Nullable List<String> values )
    {
        this.response.setHeader( CONTENT_ENCODING, values == null ? null : HEADER_MULTIVALUE_JOINER.join( values ) );
    }

    @Nullable
    @Override
    public Locale getContentLanguage()
    {
        return this.response.getLocale();
    }

    @Override
    public void setContentLanguage( @Nullable Locale value )
    {
        this.response.setLocale( value );
    }

    @Nullable
    @Override
    public Long getContentLength()
    {
        long value = this.response.getLongContentLength();
        return value < 0 ? null : value;
    }

    @Override
    public void setContentLength( @Nullable Long value )
    {
        if( value == null )
        {
            this.response.setHeader( HttpHeaders.CONTENT_LENGTH, null );
        }
        else
        {
            this.response.setLongContentLength( value );
        }
    }

    @Nullable
    @Override
    public String getContentLocation()
    {
        return getString( this.response.getHttpFields(), CONTENT_LOCATION );
    }

    @Override
    public void setContentLocation( @Nullable String value )
    {
        this.response.setHeader( CONTENT_LOCATION, value );
    }

    @Nullable
    @Override
    public String getContentMd5()
    {
        return getString( this.response.getHttpFields(), CONTENT_MD5 );
    }

    @Override
    public void setContentMd5( @Nullable String value )
    {
        this.response.setHeader( CONTENT_MD5, value );
    }

    @Nullable
    @Override
    public String getContentRange()
    {
        return getString( this.request.getHttpFields(), CONTENT_RANGE );
    }

    @Override
    public void setContentRange( @Nullable String value )
    {
        this.response.setHeader( CONTENT_RANGE, value );
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        String value = this.response.getContentType();
        return value == null ? null : MediaType.parse( value );
    }

    @Override
    public void setContentType( @Nullable MediaType value )
    {
        this.response.setContentType( value == null ? null : value.toString() );
    }

    @Nullable
    @Override
    public DateTime getExpires()
    {
        return getDateTime( this.response.getHttpFields(), EXPIRES );
    }

    @Override
    public void setExpires( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.setHeader( EXPIRES, null );
        }
        else
        {
            this.response.setDateHeader( EXPIRES, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        return getDateTime( this.response.getHttpFields(), DATE );
    }

    @Override
    public void setDate( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.setHeader( DATE, null );
        }
        else
        {
            this.response.setDateHeader( DATE, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public String getETag()
    {
        return getString( this.response.getHttpFields(), ETAG );
    }

    @Override
    public void setETag( @Nullable String value )
    {
        this.response.setHeader( ETAG, value );
    }

    @Nullable
    @Override
    public String getLocation()
    {
        return getString( this.response.getHttpFields(), LOCATION );
    }

    @Override
    public void setLocation( @Nullable String value ) throws IOException
    {
        if( value == null )
        {
            this.response.setHeader( LOCATION, null );
        }
        else
        {
            this.response.sendRedirect( value );
        }
    }

    @Nullable
    @Override
    public DateTime getLastModified()
    {
        return getDateTime( this.response.getHttpFields(), LAST_MODIFIED );
    }

    @Override
    public void setLastModified( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.setHeader( LAST_MODIFIED, null );
        }
        else
        {
            this.response.setDateHeader( LAST_MODIFIED, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return getString( this.response.getHttpFields(), PRAGMA );
    }

    @Override
    public void setPragma( @Nullable String value )
    {
        this.response.setHeader( PRAGMA, value );
    }

    @Nullable
    @Override
    public Integer getRetryAfter()
    {
        return getInteger( this.response.getHttpFields(), RETRY_AFTER );
    }

    @Override
    public void setRetryAfter( @Nullable Integer value )
    {
        if( value == null )
        {
            this.response.setHeader( RETRY_AFTER, null );
        }
        else
        {
            this.response.setIntHeader( RETRY_AFTER, value );
        }
    }

    @Nullable
    @Override
    public String getServer()
    {
        return getString( this.response.getHttpFields(), SERVER );
    }

    @Override
    public void setServer( @Nullable String value )
    {
        this.response.setHeader( SERVER, value );
    }

    @Nonnull
    @Override
    public List<String> getVary()
    {
        return getStrings( this.response.getHttpFields(), VARY );
    }

    @Override
    public void setVary( @Nullable List<String> values )
    {
        this.response.setHeader( VARY, values == null ? null : HEADER_MULTIVALUE_JOINER.join( values ) );
    }

    @Nullable
    @Override
    public String getWarning()
    {
        return getString( this.response.getHttpFields(), WARNING );
    }

    @Override
    public void setWarning( @Nullable String value )
    {
        this.response.setHeader( WARNING, value );
    }

    @Nullable
    @Override
    public String getWwwAuthenticate()
    {
        return getString( this.response.getHttpFields(), WWW_AUTHENTICATE );
    }

    @Override
    public void setWwwAuthenticate( @Nullable String value )
    {
        this.response.setHeader( WWW_AUTHENTICATE, value );
    }

    @Override
    public void addHeader( @Nonnull String name, @Nonnull String value )
    {
        this.response.addHeader( name, value );
    }

    @Override
    public void addHeader( @Nonnull String name, @Nonnull DateTime value )
    {
        this.response.addDateHeader( name, value.getMillis() );
    }

    @Override
    public void addHeader( @Nonnull String name, @Nonnull Integer value )
    {
        this.response.addIntHeader( name, value );
    }

    @Override
    public void addHeader( @Nonnull String name, @Nonnull Long value )
    {
        this.response.addHeader( name, value + "" );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull String value )
    {
        this.response.setHeader( name, value );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull DateTime value )
    {
        this.response.setDateHeader( name, value.getMillis() );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull Integer value )
    {
        this.response.setIntHeader( name, value );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull Long value )
    {
        this.response.setHeader( name, value + "" );
    }

    @Override
    public void removeHeader( @Nonnull String name )
    {
        this.response.setHeader( name, null );
    }
}
