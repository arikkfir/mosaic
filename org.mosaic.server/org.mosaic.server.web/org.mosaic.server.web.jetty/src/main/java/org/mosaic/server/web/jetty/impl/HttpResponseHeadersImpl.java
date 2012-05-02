package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.RuntimeIOException;
import org.joda.time.DateTime;
import org.mosaic.server.web.util.HttpTime;
import org.mosaic.util.collection.MissingRequiredValueException;
import org.mosaic.web.HttpCookie;
import org.mosaic.web.HttpResponseHeaders;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static java.lang.System.currentTimeMillis;
import static org.springframework.util.StringUtils.collectionToCommaDelimitedString;

/**
 * @author arik
 */
public class HttpResponseHeadersImpl implements HttpResponseHeaders
{

    private final HttpServletResponse response;

    private final ConversionService conversionService;

    public HttpResponseHeadersImpl( HttpServletResponse response, ConversionService conversionService )
    {
        this.response = response;
        this.conversionService = conversionService;
    }

    @Override
    public Set<HttpMethod> getAllow( )
    {
        Set<HttpMethod> methods = new HashSet<>( );
        String allow = getValue( "Allow" );
        if( allow != null )
        {
            for( String method : allow.split( ",\\s*" ) )
            {
                methods.add( HttpMethod.valueOf( method.toUpperCase( ) ) );
            }
        }
        return methods;
    }

    @Override
    public void setAllow( Collection<HttpMethod> methods )
    {
        put( "Allow", collectionToCommaDelimitedString( methods ) );
    }

    @Override
    public String getCacheControl( )
    {
        return getValue( "Cache-Control" );
    }

    @Override
    public void setCacheControl( String cacheControl )
    {
        put( "Cache-Control", cacheControl );
    }

    @Override
    public Locale getContentLanguage( )
    {
        return getValueAs( "Content-Language", Locale.class );
    }

    @Override
    public void setContentLanguage( Locale contentLanguage )
    {
        putAs( "Content-Language", contentLanguage );
    }

    @Override
    public Long getContentLength( )
    {
        return getValueAs( "Content-Length", Long.class );
    }

    @Override
    public void setContentLength( Long contentLength )
    {
        putAs( "Content-Length", contentLength );
    }

    @Override
    public MediaType getContentType( )
    {
        return getValueAs( "Content-Type", MediaType.class );
    }

    @Override
    public void setContentType( MediaType contentType )
    {
        putAs( "Content-Type", contentType );
    }

    @Override
    public Charset getContentCharset( )
    {
        return Charset.forName( this.response.getCharacterEncoding( ) );
    }

    @Override
    public void setContentCharset( Charset charset )
    {
        this.response.setCharacterEncoding( charset.name( ) );
    }

    @Override
    public void addCookie( HttpCookie newCookie )
    {
        Cookie cookie = new Cookie( newCookie.getName( ), newCookie.getValue( ) );

        if( newCookie.getDomain( ) != null )
        {
            cookie.setDomain( newCookie.getDomain( ) );
        }
        if( newCookie.getPath( ) != null )
        {
            cookie.setPath( newCookie.getPath( ) );
        }
        if( newCookie.getMaxAge( ) != null )
        {
            cookie.setMaxAge( newCookie.getMaxAge( ) );
        }
        cookie.setHttpOnly( newCookie.getHttpOnly( ) );
        cookie.setSecure( newCookie.getSecure( ) );
        cookie.setVersion( newCookie.getVersion( ) );
        if( newCookie.getComment( ) != null )
        {
            cookie.setComment( newCookie.getComment( ) );
        }
        this.response.addCookie( cookie );
    }

    @Override
    public String getETag( )
    {
        return getValue( "ETag" );
    }

    @Override
    public void setETag( String eTag )
    {
        put( "ETag", eTag );
    }

    @Override
    public DateTime getExpires( )
    {
        return getValueAs( "Expires", DateTime.class );
    }

    @Override
    public void setExpires( DateTime expires )
    {
        putAs( "Expires", expires );
    }

    @Override
    public void setExpires( Long secondsFromNow )
    {
        if( secondsFromNow == null )
        {

            // null value means that we don't want to send back any cache headers - let the user agent decide for itself
            this.response.setHeader( "Expires", null );
            this.response.setHeader( "Pragma", null );
            this.response.setHeader( "Cache-Control", null );

        }
        else if( secondsFromNow <= 0 )
        {

            // zero or negative means that we want to completely prevent any user agent/proxy from caching our response
            this.response.setHeader( "Pragma", "no-cache" );
            this.response.setHeader( "Expires", HttpTime.format( new DateTime( ) ) );
            this.response.setHeader( "Cache-Control", "no-cache, no-store" );

        }
        else
        {

            // any other number means we want to allow caching of this response, but only for the next specified seconds
            this.response.setHeader( "Expires", HttpTime.format( new DateTime( currentTimeMillis( ) +
                                                                               secondsFromNow * 1000l ) ) );
            this.response.setHeader( "Cache-Control", "max-age=" + secondsFromNow + ", must-revalidate" );
        }
    }

    @Override
    public DateTime getLastModified( )
    {
        return getValueAs( "Last-Modified", DateTime.class );
    }

    @Override
    public void setLastModified( DateTime lastModified )
    {
        putAs( "Last-Modified", lastModified );
    }

    @Override
    public String getLocation( )
    {
        return getValue( "Location" );
    }

    @Override
    public void setLocation( String location )
    {
        try
        {
            this.response.sendRedirect( location );
        }
        catch( IOException e )
        {
            throw new RuntimeIOException( "Error redirecting to '" + location + "': " + e.getMessage( ), e );
        }
    }

    @Override
    public String getPragma( )
    {
        return getValue( "Pragma" );
    }

    @Override
    public void setPragma( String pragma )
    {
        put( "Pragma", pragma );
    }

    @Override
    public Integer getRetryAfter( )
    {
        return getValueAs( "Retry-After", Integer.class );
    }

    @Override
    public void setRetryAfter( Integer retryAfter )
    {
        putAs( "Retry-After", retryAfter );
    }

    @Override
    public String getServer( )
    {
        return getValue( "Server" );
    }

    @Override
    public void setServer( String server )
    {
        put( "Server", server );
    }

    @Override
    public String getWwwAuthenticate( )
    {
        return getValue( "WWW-Authenticate" );
    }

    @Override
    public void setWwwAuthenticate( String wwwAuthenticate )
    {
        put( "WWW-Authenticate", wwwAuthenticate );
    }

    @Override
    public void disableCache( )
    {
        setExpires( 0l );
    }

    @Override
    public String getValue( String key )
    {
        return this.response.getHeader( key );
    }

    @Override
    public String getValue( String key, String defaultValue )
    {
        String value = getValue( key );
        return value == null ? defaultValue : value;
    }

    @Override
    public String requireValue( String key )
    {
        String value = getValue( key );
        if( value != null )
        {
            return value;
        }
        else
        {
            throw new MissingRequiredValueException( key );
        }
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type )
    {
        String value = getValue( key );
        if( value != null )
        {
            return this.conversionService.convert( value, type );
        }
        else
        {
            return null;
        }
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type, T defaultValue )
    {
        String value = getValue( key );
        if( value != null )
        {
            return this.conversionService.convert( value, type );
        }
        else
        {
            return defaultValue;
        }
    }

    @Override
    public <T> T requireValueAs( String key, Class<T> type )
    {
        return this.conversionService.convert( requireValue( key ), type );
    }

    @Override
    public void add( String key, String value )
    {
        this.response.addHeader( key, value );
    }

    @Override
    public void put( String key, String value )
    {
        this.response.setHeader( key, value );
    }

    @Override
    public <T> void addAs( String key, T value )
    {
        add( key, this.conversionService.convert( value, String.class ) );
    }

    @Override
    public <T> void putAs( String key, T value )
    {
        put( key, this.conversionService.convert( value, String.class ) );
    }

    @Override
    public Map<String, String> toMap( )
    {
        Collection<String> headerNames = this.response.getHeaderNames( );
        Map<String, String> headers = new HashMap<>( headerNames.size( ) );
        for( String headerName : headerNames )
        {
            headers.put( headerName, getValue( headerName ) );
        }
        return headers;
    }

    @Override
    public <T> Map<String, T> toMapAs( Class<T> type )
    {
        Collection<String> headerNames = this.response.getHeaderNames( );
        Map<String, T> headers = new HashMap<>( headerNames.size( ) );
        for( String headerName : headerNames )
        {
            headers.put( headerName, getValueAs( headerName, type ) );
        }
        return headers;
    }

    @Override
    public int size( )
    {
        return this.response.getHeaderNames( ).size( );
    }

    @Override
    public boolean isEmpty( )
    {
        return this.response.getHeaderNames( ).isEmpty( );
    }

    @Override
    public boolean containsKey( Object key )
    {
        return this.response.containsHeader( key.toString( ) );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return toMap( ).containsValue( value );
    }

    @Override
    public List<String> get( Object key )
    {
        return new LinkedList<>( this.response.getHeaders( key.toString( ) ) );
    }

    @Override
    public List<String> put( String key, List<String> values )
    {
        List<String> previousValues = get( key );

        this.response.setHeader( key, null );
        for( String value : values )
        {
            this.response.addHeader( key, value );
        }

        return previousValues;
    }

    @Override
    public List<String> remove( Object key )
    {
        List<String> previousValues = get( key );
        this.response.setHeader( key.toString( ), null );
        return previousValues;
    }

    @Override
    public void putAll( Map<? extends String, ? extends List<String>> map )
    {
        for( Entry<? extends String, ? extends List<String>> entry : map.entrySet( ) )
        {
            put( entry.getKey( ), entry.getValue( ) );
        }
    }

    @Override
    public void clear( )
    {
        throw new UnsupportedOperationException( "Clearing all headers is not supported (too dangerous)" );
    }

    @Override
    public Set<String> keySet( )
    {
        return new HashSet<>( this.response.getHeaderNames( ) );
    }

    @Override
    public Collection<List<String>> values( )
    {
        Collection<List<String>> values = new LinkedList<>( );
        for( String headerName : this.response.getHeaderNames( ) )
        {
            values.add( new LinkedList<>( this.response.getHeaders( headerName ) ) );
        }
        return values;
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet( )
    {
        Set<Entry<String, List<String>>> values = new HashSet<>( this.response.getHeaderNames( ).size( ) );
        for( String headerName : this.response.getHeaderNames( ) )
        {
            LinkedList<String> headerValues = new LinkedList<>( this.response.getHeaders( headerName ) );
            values.add( new AbstractMap.SimpleImmutableEntry<String, List<java.lang.String>>( headerName, headerValues ) );
        }
        return values;
    }
}
