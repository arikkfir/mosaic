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
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static java.util.Collections.unmodifiableCollection;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

/**
 * @author arik
 */
public class HttpRequestImpl extends WrappingTypedDict<Object> implements HttpRequest {

    private static final Logger LOG = LoggerFactory.getLogger( HttpRequestImpl.class );

    private static final String SESSION_ATTR = HttpSession.class.getName();

    private static final String REQUEST_ATTR = HttpServletRequest.class.getName();

    private static final String RESPONSE_ATTR = HttpServletResponse.class.getName();

    private static Map<String, List<String>> convert( Map source ) {
        Map<String, List<String>> dest = new HashMap<>();
        for( Object entryItem : source.entrySet() ) {
            Map.Entry entry = ( Entry ) entryItem;
            dest.put( ( String ) entry.getKey(), Arrays.asList( ( String[] ) entry.getValue() ) );
        }
        return dest;
    }

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final HttpApplication application;

    private final URI uri;

    private final TypedDict<String> queryParameters;

    private final HttpRequestHeadersImpl requestHeaders;

    private final Map<String, HttpPart> parts;

    private final HttpResponseHeadersImpl responseHeaders;

    public HttpRequestImpl( ConversionService conversionService,
                            HttpApplication application,
                            HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException {

        super( new HashMap<String, List<Object>>(), conversionService, Object.class );
        this.application = application;

        // delegates
        this.request = request;
        this.response = response;
        this.uri = parseRequestUri();
        put( REQUEST_ATTR, this.request );
        put( RESPONSE_ATTR, this.response );

        // headers
        this.requestHeaders = new HttpRequestHeadersImpl( this.request, this.conversionService );
        this.responseHeaders = new HttpResponseHeadersImpl( this.response, this.conversionService );

        // query parameters
        this.queryParameters = new WrappingTypedDict<>( convert( this.request.getParameterMap() ), this.conversionService, String.class );

        // multipart
        this.parts = new HashMap<>();
        for( Part part : this.request.getParts() ) {
            this.parts.put( part.getName(), new HttpPartImpl( part, this.conversionService ) );
        }
    }

    @Override
    public HttpApplication getApplication() {
        return this.application;
    }

    @Override
    public HttpSession getSession() {
        javax.servlet.http.HttpSession session = this.request.getSession();
        if( session == null ) {
            return null;
        }

        Object httpSession = session.getAttribute( SESSION_ATTR );
        if( httpSession instanceof HttpSession ) {
            return ( HttpSession ) httpSession;
        }

        LOG.warn( "Illegal HTTP session instance found in request attribute '{}': {}", SESSION_ATTR, httpSession );
        return null;
    }

    @Override
    public HttpSession getOrCreateSession() {
        HttpSession session = getSession();
        if( session == null ) {
            session = new HttpSessionImpl( this.conversionService, this.request.getSession( true ) );
            this.request.setAttribute( SESSION_ATTR, session );
        }
        return session;
    }

    @Override
    public boolean isSecure() {
        return this.request.isSecure();
    }

    @Override
    public String getClientAddress() {
        String clientAddress = this.request.getHeader( "X-Forwarded-For" );
        if( clientAddress == null ) {
            clientAddress = this.request.getRemoteAddr();
        }
        return clientAddress;
    }

    @Override
    public String getProtocol() {
        return this.request.getProtocol();
    }

    @Override
    public HttpMethod getMethod() {
        String method = this.request.getHeader( "X-Mosaic-Method" );
        if( method == null ) {
            method = this.request.getHeader( "X-Mosaic-Method-Override" );
        }
        if( method == null ) {
            method = this.request.getMethod();
        }
        return HttpMethod.valueOf( method.toUpperCase() );
    }

    @Override
    public URI getUrl() {
        return this.uri;
    }

    @Override
    public TypedDict<String> getQueryParameters() {
        return this.queryParameters;
    }

    @Override
    public HttpRequestHeaders getRequestHeaders() {
        return this.requestHeaders;
    }

    @Override
    public InputStream getRequestInputStream() throws IOException {
        return this.request.getInputStream();
    }

    @Override
    public Reader getRequestReader() throws IOException {
        return this.request.getReader();
    }

    @Override
    public HttpPart getPart( String name ) {
        return this.parts.get( name );
    }

    @Override
    public Collection<HttpPart> getParts() {
        return unmodifiableCollection( this.parts.values() );
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.valueOf( this.response.getStatus() );
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void setResponseStatus( HttpStatus status, String text ) {
        this.response.setStatus( status.value(), text );
        if( status.series() == CLIENT_ERROR || status.series() == SERVER_ERROR ) {
            this.responseHeaders.disableCache();
        }
    }

    @Override
    public HttpResponseHeaders getResponseHeaders() {
        return this.responseHeaders;
    }

    @Override
    public OutputStream getResponseOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

    @Override
    public Writer getResponseWriter() throws IOException {
        return this.response.getWriter();
    }

    @Override
    public boolean isCommitted() {
        return this.response.isCommitted();
    }

    private URI parseRequestUri() throws UnsupportedEncodingException {
        try {
            String queryString = this.request.getQueryString();
            if( queryString != null ) {
                queryString = URLDecoder.decode( queryString, "UTF-8" );
            }
            return new URI( this.request.getScheme(),
                            null,
                            this.request.getServerName(),
                            this.request.getServerPort(),
                            this.request.getPathInfo(),
                            queryString,
                            null );
        } catch( URISyntaxException e ) {
            throw new IllegalArgumentException( "Cannot parse URI '" + this.request.getRequestURL() + "': " + e.getMessage(), e );
        }
    }
}
