package org.mosaic.server.web.jetty.impl;

import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.mosaic.collection.TypedDict;
import org.mosaic.collection.WrappingTypedDict;
import org.mosaic.web.HttpCookie;
import org.mosaic.web.HttpResponseHeaders;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.util.StringUtils.collectionToCommaDelimitedString;

/**
 * @author arik
 */
public class HttpResponseHeadersImpl implements HttpResponseHeaders {

    private final HttpServletResponse response;

    private final TypedDict<String> headers;

    public HttpResponseHeadersImpl( HttpServletResponse response, ConversionService conversionService ) {
        this.response = response;
        this.headers = new WrappingTypedDict<>( new HashMap<String, List<String>>( 20 ), conversionService, String.class );
    }

    @Override
    public Set<HttpMethod> getAllow() {
        Set<HttpMethod> methods = new HashSet<>();
        String allow = getValue( "Allow" );
        if( allow != null ) {
            for( String method : allow.split( ",\\s*" ) ) {
                methods.add( HttpMethod.valueOf( method.toUpperCase() ) );
            }
        }
        return methods;
    }

    @Override
    public void setAllow( Collection<HttpMethod> methods ) {
        put( "Allow", collectionToCommaDelimitedString( methods ) );
    }

    @Override
    public String getCacheControl() {
        return getValue( "Cache-Control" );
    }

    @Override
    public void setCacheControl( String cacheControl ) {
        put( "Cache-Control", cacheControl );
    }

    @Override
    public Locale getContentLanguage() {
        return getValueAs( "Content-Language", Locale.class );
    }

    @Override
    public void setContentLanguage( Locale contentLanguage ) {
        putAs( "Content-Language", contentLanguage );
    }

    @Override
    public Long getContentLength() {
        return getValueAs( "Content-Length", Long.class );
    }

    @Override
    public void setContentLength( Long contentLength ) {
        putAs( "Content-Length", contentLength );
    }

    @Override
    public MediaType getContentType() {
        return getValueAs( "Content-Type", MediaType.class );
    }

    @Override
    public void setContentType( MediaType contentType ) {
        putAs( "Content-Type", contentType );
    }

    @Override
    public void addCookie( HttpCookie newCookie ) {
        Cookie cookie = new Cookie( newCookie.getName(), newCookie.getValue() );

        if( newCookie.getDomain() != null ) {
            cookie.setDomain( newCookie.getDomain() );
        }
        if( newCookie.getPath() != null ) {
            cookie.setPath( newCookie.getPath() );
        }
        if( newCookie.getMaxAge() != null ) {
            cookie.setMaxAge( newCookie.getMaxAge() );
        }
        cookie.setHttpOnly( newCookie.getHttpOnly() );
        cookie.setSecure( newCookie.getSecure() );
        cookie.setVersion( newCookie.getVersion() );
        if( newCookie.getComment() != null ) {
            cookie.setComment( newCookie.getComment() );
        }
        this.response.addCookie( cookie );
    }

    @Override
    public String getETag() {
        return getValue( "ETag" );
    }

    @Override
    public void setETag( String eTag ) {
        put( "ETag", eTag );
    }

    @Override
    public DateTime getExpires() {
        return getValueAs( "Expires", DateTime.class );
    }

    @Override
    public void setExpires( DateTime expires ) {
        putAs( "Expires", expires );
    }

    @Override
    public DateTime getLastModified() {
        return getValueAs( "Last-Modified", DateTime.class );
    }

    @Override
    public void setLastModified( DateTime lastModified ) {
        putAs( "Last-Modified", lastModified );
    }

    @Override
    public String getLocation() {
        return getValue( "Location" );
    }

    @Override
    public void setLocation( String location ) {
        put( "Location", location );
    }

    @Override
    public String getPragma() {
        return getValue( "Pragma" );
    }

    @Override
    public void setPragma( String pragma ) {
        put( "Pragma", pragma );
    }

    @Override
    public Integer getRetryAfter() {
        return getValueAs( "Retry-After", Integer.class );
    }

    @Override
    public void setRetryAfter( Integer retryAfter ) {
        putAs( "Retry-After", retryAfter );
    }

    @Override
    public String getServer() {
        return getValue( "Server" );
    }

    @Override
    public void setServer( String server ) {
        put( "Server", server );
    }

    @Override
    public String getWwwAuthenticate() {
        return getValue( "WWW-Authenticate" );
    }

    @Override
    public void setWwwAuthenticate( String wwwAuthenticate ) {
        put( "WWW-Authenticate", wwwAuthenticate );
    }

    @Override
    public String getValue( String key ) {
        return this.headers.getValue( key );
    }

    @Override
    public String getValue( String key, String defaultValue ) {
        return this.headers.getValue( key, defaultValue );
    }

    @Override
    public String requireValue( String key ) {
        return this.headers.requireValue( key );
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type ) {
        return this.headers.getValueAs( key, type );
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type, T defaultValue ) {
        return this.headers.getValueAs( key, type, defaultValue );
    }

    @Override
    public <T> T requireValueAs( String key, Class<T> type ) {
        return this.headers.requireValueAs( key, type );
    }

    @Override
    public void add( String key, String value ) {
        this.headers.add( key, value );
    }

    @Override
    public void put( String key, String value ) {
        this.headers.put( key, value );
    }

    @Override
    public <T> void addAs( String key, T value ) {
        this.headers.addAs( key, value );
    }

    @Override
    public <T> void putAs( String key, T value ) {
        this.headers.putAs( key, value );
    }

    @Override
    public Map<String, String> toMap() {
        return this.headers.toMap();
    }

    @Override
    public <T> Map<String, T> toMapAs( Class<T> type ) {
        return this.headers.toMapAs( type );
    }

    @Override
    public int size() {
        return this.headers.size();
    }

    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {
        return this.headers.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value ) {
        return this.headers.containsValue( value );
    }

    @Override
    public List<String> get( Object key ) {
        return this.headers.get( key );
    }

    @Override
    public List<String> put( String key, List<String> value ) {
        return this.headers.put( key, value );
    }

    @Override
    public List<String> remove( Object key ) {
        return this.headers.remove( key );
    }

    @Override
    public void putAll( Map<? extends String, ? extends List<String>> m ) {
        this.headers.putAll( m );
    }

    @Override
    public void clear() {
        this.headers.clear();
    }

    @Override
    public Set<String> keySet() {
        return this.headers.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        return this.headers.values();
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return this.headers.entrySet();
    }
}
