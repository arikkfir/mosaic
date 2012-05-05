package org.mosaic.server.web.jetty.impl;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.mosaic.server.web.PathParamsAware;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.collection.MapWrapper;
import org.mosaic.util.collection.MultiMapAccessor;
import org.mosaic.util.collection.MultiMapWrapper;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static org.mosaic.util.collection.Maps.listMapFromArrayMap;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

/**
 * @author arik
 */
public class HttpRequestImpl implements HttpRequest, PathParamsAware
{
    private static final Logger LOG = LoggerFactory.getLogger( HttpRequestImpl.class );

    private static final String SESSION_ATTR = HttpSession.class.getName();

    private static final String REQUEST_ATTR = HttpServletRequest.class.getName();

    private static final String RESPONSE_ATTR = HttpServletResponse.class.getName();

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final HttpApplication application;

    private final URI uri;

    private final MultiMapWrapper<String, String> queryParameters;

    private final MapWrapper<String, Object> attributes;

    private final MapWrapper<String, String> pathParameters;

    private final HttpRequestHeadersImpl requestHeaders;

    private final Map<String, HttpPart> parts;

    private final HttpResponseHeadersImpl responseHeaders;

    public HttpRequestImpl( HttpApplication application,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            ConversionService conversionService ) throws IOException, ServletException
    {
        this.application = application;
        this.attributes = new MapWrapper<>( new HashMap<String, Object>(), conversionService );

        // delegates
        this.request = request;
        this.response = response;
        this.uri = parseRequestUri();
        put( REQUEST_ATTR, this.request );
        put( RESPONSE_ATTR, this.response );

        // headers
        this.requestHeaders = new HttpRequestHeadersImpl( this.request );
        this.responseHeaders = new HttpResponseHeadersImpl( this.response );

        // query parameters
        this.queryParameters = new MultiMapWrapper<>( unmodifiableMap( listMapFromArrayMap( this.request.getParameterMap() ) ), conversionService );
        this.pathParameters = new MapWrapper<>( Collections.<String, String>emptyMap(), conversionService );

        // multi-part
        this.parts = new HashMap<>();
        for( Part part : this.request.getParts() )
        {
            this.parts.put( part.getName(), new HttpPartImpl( part ) );
        }
    }

    @Override
    public HttpApplication getApplication()
    {
        return this.application;
    }

    @Override
    public HttpSession getSession()
    {
        javax.servlet.http.HttpSession servletSession = this.request.getSession();
        if( servletSession == null )
        {
            return null;
        }

        Object httpSession = servletSession.getAttribute( SESSION_ATTR );
        if( httpSession instanceof HttpSession )
        {
            return ( HttpSession ) httpSession;
        }

        LOG.warn( "Illegal HTTP session instance found in request attribute '{}': {}", SESSION_ATTR, httpSession );
        return null;
    }

    @Override
    public HttpSession getOrCreateSession()
    {
        javax.servlet.http.HttpSession servletSession = this.request.getSession( true );
        if( servletSession == null )
        {
            throw new IllegalStateException( "Could not create HTTP session - underlying Servlet container refused to create the session" );
        }

        Object httpSession = servletSession.getAttribute( SESSION_ATTR );
        if( httpSession == null )
        {
            httpSession = new HttpSessionImpl( servletSession, this.attributes.getConversionService() );
            servletSession.setAttribute( SESSION_ATTR, httpSession );
            return ( HttpSession ) httpSession;
        }
        else if( httpSession instanceof HttpSession )
        {
            return ( HttpSession ) httpSession;
        }
        else
        {
            throw new IllegalStateException( String.format( "Illegal type of HTTP session object stored in Servlet session object: %s", httpSession ) );
        }
    }

    @Override
    public boolean isSecure()
    {
        return this.request.isSecure();
    }

    @Override
    public String getClientAddress()
    {
        String clientAddress = this.request.getHeader( "X-Forwarded-For" );
        if( clientAddress == null )
        {
            clientAddress = this.request.getRemoteAddr();
        }
        return clientAddress;
    }

    @Override
    public String getProtocol()
    {
        return this.request.getProtocol();
    }

    @Override
    public HttpMethod getMethod()
    {
        String method = this.request.getHeader( "X-Mosaic-Method" );
        if( method == null )
        {
            method = this.request.getHeader( "X-Mosaic-Method-Override" );
        }
        if( method == null )
        {
            method = this.request.getMethod();
        }
        return HttpMethod.valueOf( method.toUpperCase() );
    }

    @Override
    public URI getUrl()
    {
        return this.uri;
    }

    @Override
    public MultiMapAccessor<String, String> getQueryParameters()
    {
        return this.queryParameters;
    }

    @Override
    public MapAccessor<String, String> getPathParameters()
    {
        return this.pathParameters;
    }

    @Override
    public void setPathParams( Map<String, String> params )
    {
        this.pathParameters.setMap( Collections.unmodifiableMap( new HashMap<>( params ) ) );
    }

    @Override
    public HttpRequestHeaders getRequestHeaders()
    {
        return this.requestHeaders;
    }

    @Override
    public InputStream getRequestInputStream() throws IOException
    {
        return this.request.getInputStream();
    }

    @Override
    public Reader getRequestReader() throws IOException
    {
        return this.request.getReader();
    }

    @Override
    public HttpPart getPart( String name )
    {
        return this.parts.get( name );
    }

    @Override
    public Collection<HttpPart> getParts()
    {
        return unmodifiableCollection( this.parts.values() );
    }

    @Override
    public HttpStatus getResponseStatus()
    {
        return HttpStatus.valueOf( this.response.getStatus() );
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void setResponseStatus( HttpStatus status, String text )
    {
        this.response.setStatus( status.value(), text );
        if( status.series() == CLIENT_ERROR || status.series() == SERVER_ERROR )
        {
            this.responseHeaders.disableCache();
        }
    }

    @Override
    public HttpResponseHeaders getResponseHeaders()
    {
        return this.responseHeaders;
    }

    @Override
    public OutputStream getResponseOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Override
    public Writer getResponseWriter() throws IOException
    {
        return this.response.getWriter();
    }

    @Override
    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    @Override
    public <T> T get( String key, Class<T> type, T defaultValue )
    {
        return attributes.get( key, type, defaultValue );
    }

    @Override
    public <T> T require( String key, Class<T> type )
    {
        return attributes.require( key, type );
    }

    @Override
    public <T> T get( String key, Class<T> type )
    {
        return attributes.get( key, type );
    }

    @Override
    public Object require( String key )
    {
        return attributes.require( key );
    }

    @Override
    public Object get( String key, Object defaultValue )
    {
        return attributes.get( key, defaultValue );
    }

    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return attributes.entrySet();
    }

    @Override
    public Collection<Object> values()
    {
        return attributes.values();
    }

    @Override
    public Set<String> keySet()
    {
        return attributes.keySet();
    }

    @Override
    public void clear()
    {
        attributes.clear();
    }

    @Override
    public void putAll( Map<? extends String, ?> m )
    {
        attributes.putAll( m );
    }

    @Override
    public Object remove( Object key )
    {
        return attributes.remove( key );
    }

    @Override
    public Object put( String key, Object value )
    {
        return attributes.put( key, value );
    }

    @Override
    public Object get( Object key )
    {
        return attributes.get( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return attributes.containsValue( value );
    }

    @Override
    public boolean containsKey( Object key )
    {
        return attributes.containsKey( key );
    }

    @Override
    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    @Override
    public int size()
    {
        return attributes.size();
    }

    private URI parseRequestUri() throws UnsupportedEncodingException
    {
        try
        {
            String queryString = this.request.getQueryString();
            if( queryString != null )
            {
                queryString = URLDecoder.decode( queryString, "UTF-8" );
            }
            return new URI( this.request.getScheme(), null, this.request.getServerName(), this.request.getServerPort(), this.request.getPathInfo(), queryString, null );
        }
        catch( URISyntaxException e )
        {
            throw new IllegalArgumentException( "Cannot parse URI '" +
                                                this.request.getRequestURL() +
                                                "': " +
                                                e.getMessage(), e );
        }
    }
}
