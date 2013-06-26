package org.mosaic.web.request.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Response;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebResponse;

import static java.util.Collections.emptySet;

/**
 * @author arik
 */
public class WebResponseImpl implements WebResponse, WebResponse.Headers
{
    @Nonnull
    private final WebRequestImpl request;

    @Nonnull
    private final Response response;

    public WebResponseImpl( @Nonnull WebRequestImpl request, @Nonnull Response response )
    {
        this.request = request;
        this.response = response;
    }

    @Override
    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    @Nullable
    @Override
    public HttpStatus getStatus()
    {
        return HttpStatus.valueOf( this.response.getStatus() );
    }

    @Override
    public void setStatus( @Nonnull HttpStatus status )
    {
        this.response.setStatus( status.value() );
    }

    @Override
    public void setStatus( @Nonnull HttpStatus status, @Nullable String text )
    {
        this.response.setStatusWithReason( status.value(), text );
    }

    @Nonnull
    @Override
    public OutputStream getBinaryBody() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Nonnull
    @Override
    public Writer getCharacterBody() throws IOException
    {
        return this.response.getWriter();
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value )
    {
        addCookie( name, value, this.request.getHost(), this.request.getEncodedPath() );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain )
    {
        addCookie( name, value, domain, this.request.getEncodedPath() );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain, @Nonnull String path )
    {
        this.response.addCookie( new HttpCookie( name, Objects.toString( value, null ), domain, path ) );
    }

    @Override
    public void addCookie( @Nonnull String name,
                           @Nonnull Object value,
                           @Nonnull String domain,
                           @Nonnull String path,
                           @Nonnull Period maxAge )
    {
        this.response.addCookie( new HttpCookie( name,
                                                 Objects.toString( value, null ),
                                                 domain,
                                                 path,
                                                 maxAge.toStandardSeconds().getSeconds(),
                                                 false,
                                                 false ) );
    }

    @Override
    public void removeCookie( @Nonnull String name )
    {
        this.response.addCookie( new HttpCookie( name, null ) );
    }

    @Override
    public void disableCaching()
    {
        setCacheControl( "no-cache, no-store, must-revalidate" );
        setExpires( new DateTime() );
    }

    @Override
    public void allowPublicCaches( @Nonnull Period maxAge )
    {
        setCacheControl( "public, max-age=" + maxAge.toStandardSeconds().getSeconds() + ", must-revalidate" );
        setExpires( new DateTime().plus( maxAge ) );
    }

    @Override
    public void allowOnlyPrivateCaches( @Nonnull Period maxAge )
    {
        setCacheControl( "private, max-age=" + maxAge.toStandardSeconds().getSeconds() + ", must-revalidate" );
        setExpires( new DateTime().plus( maxAge ) );
    }

    @Nonnull
    @Override
    public Headers getHeaders()
    {
        return this;
    }

    @Nullable
    @Override
    public Collection<HttpMethod> getAllow()
    {
        Enumeration<String> values = this.response.getHttpFields().getValues( HttpHeader.ALLOW.toString(), HttpFields.__separators );
        if( values != null )
        {
            Collection<HttpMethod> methods = new LinkedHashSet<>( 5 );
            while( values.hasMoreElements() )
            {
                String value = values.nextElement();
                methods.add( HttpMethod.valueOf( value.toUpperCase() ) );
            }
            return methods;
        }
        return emptySet();
    }

    @Override
    public void setAllow( @Nullable Collection<HttpMethod> value )
    {
        if( value != null )
        {
            StringBuilder buffer = new StringBuilder( 50 );
            for( HttpMethod method : value )
            {
                if( buffer.length() > 0 )
                {
                    buffer.append( ", " );
                }
                buffer.append( method );
            }
            this.response.getHttpFields().put( HttpHeader.ALLOW, buffer.toString() );
        }
        else
        {
            this.response.getHttpFields().remove( HttpHeader.ALLOW );
        }
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.CACHE_CONTROL );
    }

    @Override
    public void setCacheControl( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.CACHE_CONTROL, value );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.CONNECTION );
    }

    @Override
    public void setConnection( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.CONNECTION, value );
    }

    @Nullable
    @Override
    public Locale getContentLanguage()
    {
        String value = this.response.getHttpFields().getStringField( HttpHeader.CONTENT_LANGUAGE );
        return value == null ? null : new Locale( value );
    }

    @Override
    public void setContentLanguage( @Nullable Locale value )
    {
        this.response.getHttpFields().put( HttpHeader.CONTENT_LANGUAGE, Objects.toString( value, null ) );
    }

    @Nullable
    @Override
    public Long getContentLength()
    {
        long value = this.response.getHttpFields().getLongField( HttpHeader.CONTENT_LENGTH.toString() );
        return value < 0 ? null : value;
    }

    @Override
    public void setContentLength( @Nullable Long value )
    {
        if( value == null )
        {
            this.response.getHttpFields().remove( HttpHeader.CONTENT_LENGTH );
        }
        else
        {
            this.response.getHttpFields().putLongField( HttpHeader.CONTENT_LENGTH, value );
        }
    }

    @Nullable
    @Override
    public String getContentLocation()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.CONTENT_LOCATION );
    }

    @Override
    public void setContentLocation( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.CONTENT_LOCATION, value );
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        String value = this.response.getHttpFields().getStringField( HttpHeader.CONTENT_TYPE );
        return value == null ? null : new MediaType( value );
    }

    @Override
    public void setContentType( @Nullable MediaType value )
    {
        this.response.getHttpFields().put( HttpHeader.CONTENT_TYPE, Objects.toString( value, null ) );
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        long value = this.response.getHttpFields().getDateField( HttpHeader.DATE.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Override
    public void setDate( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.getHttpFields().remove( HttpHeader.DATE );
        }
        else
        {
            this.response.getHttpFields().putDateField( HttpHeader.DATE, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public String getETag()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.ETAG );
    }

    @Override
    public void setETag( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.ETAG, value );
    }

    @Nullable
    @Override
    public DateTime getExpires()
    {
        long value = this.response.getHttpFields().getDateField( HttpHeader.EXPIRES.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Override
    public void setExpires( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.getHttpFields().remove( HttpHeader.EXPIRES );
        }
        else
        {
            this.response.getHttpFields().putDateField( HttpHeader.EXPIRES, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public DateTime getLastModified()
    {
        long value = this.response.getHttpFields().getDateField( HttpHeader.LAST_MODIFIED.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Override
    public void setLastModified( @Nullable DateTime value )
    {
        if( value == null )
        {
            this.response.getHttpFields().remove( HttpHeader.LAST_MODIFIED );
        }
        else
        {
            this.response.getHttpFields().putDateField( HttpHeader.LAST_MODIFIED, value.getMillis() );
        }
    }

    @Nullable
    @Override
    public String getLocation()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.LOCATION );
    }

    @Override
    public void setLocation( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.LOCATION, value );
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.PRAGMA );
    }

    @Override
    public void setPragma( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.PRAGMA, value );
    }

    @Nullable
    @Override
    public Integer getRetryAfter()
    {
        long value = this.response.getHttpFields().getLongField( HttpHeader.RETRY_AFTER.toString() );
        return value < 0 ? null : ( int ) value;
    }

    @Override
    public void setRetryAfter( @Nullable Integer value )
    {
        if( value == null )
        {
            this.response.getHttpFields().remove( HttpHeader.RETRY_AFTER );
        }
        else
        {
            this.response.getHttpFields().putLongField( HttpHeader.RETRY_AFTER, value );
        }
    }

    @Nullable
    @Override
    public String getServer()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.SERVER );
    }

    @Override
    public void setServer( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.SERVER, value );
    }

    @Nullable
    @Override
    public String getWwwAuthenticate()
    {
        return this.response.getHttpFields().getStringField( HttpHeader.WWW_AUTHENTICATE );
    }

    @Override
    public void setWwwAuthenticate( @Nullable String value )
    {
        this.response.getHttpFields().put( HttpHeader.WWW_AUTHENTICATE, value );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull String value )
    {
        this.response.getHttpFields().put( name, value );
    }

    @Override
    public void setHeader( @Nonnull String name, @Nonnull Collection<String> values )
    {
        this.response.getHttpFields().put( name, new LinkedList<>( values ) );
    }
}
