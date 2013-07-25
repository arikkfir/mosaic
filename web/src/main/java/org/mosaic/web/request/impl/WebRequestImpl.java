package org.mosaic.web.request.impl;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.joda.time.DateTime;
import org.mosaic.security.User;
import org.mosaic.security.UserManager;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.padStart;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static org.eclipse.jetty.http.HttpHeader.ACCEPT_LANGUAGE;
import static org.eclipse.jetty.util.URIUtil.canonicalPath;
import static org.mosaic.web.handler.impl.util.PathParametersCompiler.NO_MATCH;
import static org.mosaic.web.request.impl.WebSessionImpl.WEB_SESSION_ATTR_KEY;

/**
 * @author arik
 */
public class WebRequestImpl implements WebRequest, WebRequest.Headers, WebRequest.Body
{
    private static final Logger LOG = LoggerFactory.getLogger( WebRequestImpl.class );

    private static final List<MediaType> DEFAULT_ACCEPT = Arrays.asList( MediaType.PLAIN_TEXT );

    private static final List<Charset> DEFAULT_CHARSET = Arrays.asList( Charset.forName( "UTF-8" ) );

    @Nonnull
    private final HashMapEx<String, Object> attributes;

    @Nonnull
    private final Request request;

    @Nonnull
    private final WebResponseImpl response;

    @Nonnull
    private final WebApplication application;

    @Nonnull
    private final String remoteAddr;

    @Nonnull
    private final UserManager userManager;

    @Nonnull
    private final String protocol;

    @Nonnull
    private final HttpMethod method;

    @Nonnull
    private final Uri uri;

    @Nonnull
    private final Map<String, RequestCookieImpl> cookies;

    @Nonnull
    private final Multimap<String, String> headers;

    @Nonnull
    private final WebDeviceImpl device;

    public WebRequestImpl( @Nonnull Request request,
                           @Nonnull ConversionService conversionService,
                           @Nonnull WebApplication application,
                           @Nonnull UserManager userManager,
                           @Nonnull LoadingCache<Pair<String, String>, MapEx<String, String>> pathTemplatesCache )
            throws UnsupportedHttpMethodException
    {
        this.attributes = new HashMapEx<>( 10, conversionService );
        this.request = request;
        this.response = new WebResponseImpl( this, request.getResponse() );
        this.application = application;
        this.protocol = this.request.getProtocol();
        this.userManager = userManager;
        this.remoteAddr = this.request.getRemoteAddr();

        try
        {
            HttpMethod method = HttpMethod.valueOf( this.request.getMethod().toUpperCase() );
            if( method == HttpMethod.HEAD )
            {
                method = HttpMethod.GET;
            }
            this.method = method;
        }
        catch( IllegalArgumentException e )
        {
            throw new UnsupportedHttpMethodException( "Unsupported HTTP method: " + this.request.getMethod() );
        }

        this.uri = new UriImpl( pathTemplatesCache );

        Cookie[] requestCookies = this.request.getCookies();
        if( requestCookies == null )
        {
            this.cookies = Collections.emptyMap();
        }
        else
        {
            Map<String, RequestCookieImpl> cookies = new HashMap<>();
            for( Cookie cookie : requestCookies )
            {
                cookies.put( cookie.getName(), new RequestCookieImpl( cookie ) );
            }
            this.cookies = cookies;
        }

        ListMultimap<String, String> headers = ArrayListMultimap.create( 20, 1 );
        for( String headerName : list( this.request.getHeaderNames() ) )
        {
            Collection<String> values = this.request.getHttpFields().getValuesCollection( headerName );
            headers.putAll( headerName, values );
        }

        if( this.application.isUriLanguageSelectionEnabled() )
        {
            for( String supportedLanguage : this.application.getContentLanguages() )
            {
                String prefix = "/" + supportedLanguage;
                if( this.uri.getDecodedPath().startsWith( prefix + "/" ) || this.uri.getDecodedPath().equals( prefix ) )
                {
                    headers.get( ACCEPT_LANGUAGE.toString() ).add( 0, supportedLanguage + ";q=1" );
                    break;
                }
            }
        }
        if( headers.get( ACCEPT_LANGUAGE.toString() ).isEmpty() )
        {
            headers.get( ACCEPT_LANGUAGE.toString() ).add( this.application.getDefaultLanguage() + ";q=1" );
        }

        this.headers = headers;

        this.device = new WebDeviceImpl();
    }

    @Override
    public String toString()
    {
        return "WebRequest[" + getUri() + "]";
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return this.attributes;
    }

    @Nonnull
    @Override
    public WebApplication getApplication()
    {
        return this.application;
    }

    @Nonnull
    @Override
    public String getClientAddress()
    {
        return this.remoteAddr;
    }

    @Nonnull
    @Override
    public WebDevice getDevice()
    {
        return this.device;
    }

    @Nonnull
    @Override
    public String getProtocol()
    {
        return this.protocol;
    }

    @Nonnull
    @Override
    public HttpMethod getMethod()
    {
        return this.method;
    }

    @Nonnull
    @Override
    public Uri getUri()
    {
        return this.uri;
    }

    @Nonnull
    @Override
    public Body getBody()
    {
        return this;
    }

    @Nonnull
    @Override
    public User getUser()
    {
        return this.userManager.getUser();
    }

    @Nullable
    @Override
    public WebSession getSession()
    {
        HttpSession session = this.request.getSession( false );
        if( session == null )
        {
            return null;
        }

        Object webSessionAttr = session.getAttribute( WEB_SESSION_ATTR_KEY );
        if( webSessionAttr == null )
        {
            return null;
        }
        else if( !WebSession.class.isInstance( webSessionAttr ) )
        {
            LOG.warn( "Non-Mosaic web session found in Jetty HTTP session, under the Mosaic web session attribute key '{}': {}", WEB_SESSION_ATTR_KEY, webSessionAttr );
            return null;
        }

        return ( WebSession ) webSessionAttr;
    }

    @Nonnull
    @Override
    public WebSession getOrCreateSession()
    {
        HttpSession session = this.request.getSession( true );

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized( session )
        {
            Object webSessionAttr = session.getAttribute( WEB_SESSION_ATTR_KEY );
            if( webSessionAttr != null )
            {
                if( !WebSession.class.isInstance( webSessionAttr ) )
                {
                    throw new IllegalStateException( "Non-Mosaic web session found in Jetty HTTP session, under the Mosaic web session attribute key '" + WEB_SESSION_ATTR_KEY + "': " + webSessionAttr );
                }
                else
                {
                    return ( WebSession ) webSessionAttr;
                }
            }
            return new WebSessionImpl( session, this.attributes.getConversionService() );
        }
    }

    @Nonnull
    @Override
    public InputStream asStream() throws IOException
    {
        return this.request.getInputStream();
    }

    @Nonnull
    @Override
    public Reader asReader() throws IOException
    {
        return this.request.getReader();
    }

    @Nullable
    @Override
    public WebPart getPart( @Nonnull String name ) throws IOException
    {
        try
        {
            return new WebPartImpl( this.request.getPart( name ) );
        }
        catch( ServletException e )
        {
            return null;
        }
    }

    @Nonnull
    @Override
    public Map<String, WebPart> getPartsMap() throws IOException
    {
        try
        {
            Map<String, WebPart> partsMap = new HashMap<>();
            for( Part part : this.request.getParts() )
            {
                partsMap.put( part.getName(), new WebPartImpl( part ) );
            }
            return partsMap;
        }
        catch( ServletException e )
        {
            return Collections.emptyMap();
        }
    }

    @Nonnull
    @Override
    public WebResponse getResponse()
    {
        return this.response;
    }

    @Override
    public void dumpToTraceLog( @Nullable String message, @Nullable Object... arguments )
    {
        LOG.trace( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToDebugLog( @Nullable String message, @Nullable Object... arguments )
    {
        LOG.debug( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToInfoLog( @Nullable String message, @Nullable Object... arguments )
    {
        LOG.info( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToWarnLog( @Nullable String message, @Nullable Object... arguments )
    {
        LOG.warn( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToErrorLog( @Nullable String message, @Nullable Object... arguments )
    {
        LOG.error( message + "\n" + getDebugString(), arguments );
    }

    @Nonnull
    @Override
    public List<MediaType> getAccept()
    {
        return getOrderedHeaderValues( MediaType.class, HttpHeader.ACCEPT, DEFAULT_ACCEPT );
    }

    @Nonnull
    @Override
    public List<Charset> getAccepCharset()
    {
        return getOrderedHeaderValues( Charset.class, HttpHeader.ACCEPT_CHARSET, DEFAULT_CHARSET );
    }

    @Nonnull
    @Override
    public List<Locale> getAcceptLanguage()
    {
        return list( this.request.getLocales() );
    }

    @Nullable
    @Override
    public String getAuthorization()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.AUTHORIZATION );
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.CACHE_CONTROL );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.CONNECTION );
    }

    @Nullable
    @Override
    public Long getContentLength()
    {
        long value = this.request.getHttpFields().getLongField( HttpHeader.CONTENT_LENGTH.toString() );
        return value < 0 ? null : value;
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        String value = this.request.getHttpFields().getStringField( HttpHeader.CONTENT_TYPE );
        return value == null ? null : this.attributes.getConversionService().convert( value, MediaType.class );
    }

    @Nullable
    @Override
    public RequestCookie getCookie( String name )
    {
        return this.cookies.get( name );
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        long value = this.request.getHttpFields().getDateField( HttpHeader.DATE.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Nullable
    @Override
    public String getExpect()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.EXPECT );
    }

    @Nullable
    @Override
    public String getFrom()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.FROM );
    }

    @Nonnull
    @Override
    public String getHost()
    {
        return this.uri.getHost();
    }

    @Nonnull
    @Override
    public Set<String> getIfMatch()
    {
        return getHeaderValuesSet( String.class, HttpHeader.IF_MATCH, Collections.<String>emptySet() );
    }

    @Nullable
    @Override
    public DateTime getIfModifiedSince()
    {
        long value = this.request.getHttpFields().getDateField( HttpHeader.IF_MODIFIED_SINCE.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Nonnull
    @Override
    public Set<String> getIfNoneMatch()
    {
        return getHeaderValuesSet( String.class, HttpHeader.IF_MATCH, Collections.<String>emptySet() );
    }

    @Nullable
    @Override
    public DateTime getIfUnmodifiedSince()
    {
        long value = this.request.getHttpFields().getDateField( HttpHeader.IF_UNMODIFIED_SINCE.toString() );
        return value < 0 ? null : new DateTime( value );
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.PRAGMA );
    }

    @Nullable
    @Override
    public String getReferer()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.REFERER );
    }

    @Nullable
    @Override
    public String getUserAgent()
    {
        return this.request.getHttpFields().getStringField( HttpHeader.USER_AGENT );
    }

    @Nonnull
    @Override
    public Headers getHeaders()
    {
        return this;
    }

    @Nonnull
    @Override
    public Multimap<String, String> getAllHeaders()
    {
        return this.headers;
    }

    @Nonnull
    private String getDebugString()
    {
        StringBuilder buffer = new StringBuilder( 5000 );
        buffer.append( "\n" );
        buffer.append( "GENERAL INFORMATION\n" );
        buffer.append( "                      Method: " ).append( getMethod() ).append( "\n" );
        buffer.append( "                   Jetty URL: " ).append( this.request.getUri() ).append( "\n" );
        buffer.append( "                      Scheme: " ).append( this.uri.getScheme() ).append( "\n" );
        buffer.append( "                        Host: " ).append( getHost() ).append( "\n" );
        buffer.append( "                        Port: " ).append( this.uri.getPort() ).append( "\n" );
        buffer.append( "                Decoded path: " ).append( this.uri.getDecodedPath() ).append( "\n" );
        buffer.append( "                Encoded path: " ).append( this.uri.getEncodedPath() ).append( "\n" );
        buffer.append( "               Encoded query: " ).append( this.uri.getEncodedQueryString() ).append( "\n" );
        buffer.append( "        Decoded query params: " ).append( this.uri.getDecodedQueryParameters().isEmpty() ? "" : this.uri.getDecodedQueryParameters() ).append( "\n" );
        buffer.append( "                    Fragment: " ).append( this.uri.getFragment() ).append( "\n" );
        buffer.append( "\n" );
        buffer.append( "CLIENT INFORMATION\n" );
        buffer.append( "        Client address: " ).append( getClientAddress() ).append( "\n" );
        buffer.append( "               Session: " ).append( getSession() ).append( "\n" );
        buffer.append( "                Device: " ).append( getDevice() ).append( "\n" );
        buffer.append( "                  User: " ).append( getUser() ).append( "\n" );
        buffer.append( "\n" );
        buffer.append( "HEADERS\n" );

        for( String headerName : list( this.request.getHeaderNames() ) )
        {
            ArrayList<String> values = list( this.request.getHeaders( headerName ) );

            headerName = padStart( headerName, 20, ' ' );
            buffer.append( "        " ).append( padStart( headerName, 20, ' ' ) ).append( ": " );

            if( values.isEmpty() )
            {
                buffer.append( "\n" );
            }
            else
            {
                boolean first = true;
                for( String value : values )
                {
                    if( first )
                    {
                        first = false;
                    }
                    else
                    {
                        buffer.append( ", " );
                    }
                    buffer.append( value ).append( "\n" );
                }
            }
        }
        return buffer.toString().replace( "{}", "\\{}" );
    }

    @Nonnull
    private Multimap<String, String> extractDecodedQueryParameters()
    {
        Map<String, String[]> parameterMap = this.request.getParameterMap();
        Multimap<String, String> decodedQueryParameters = LinkedListMultimap.create( parameterMap.size() );
        for( Map.Entry<String, String[]> entry : parameterMap.entrySet() )
        {
            decodedQueryParameters.putAll( entry.getKey(), asList( entry.getValue() ) );
        }
        return decodedQueryParameters;
    }

    @Nonnull
    private <T> Set<T> getHeaderValuesSet( @Nonnull Class<T> type,
                                           @Nonnull HttpHeader header,
                                           @Nonnull Set<T> defaultValues )
    {
        // parse all values for the header
        Enumeration<String> rawValues = this.request.getHttpFields().getValues( header.toString(), HttpFields.__separators );
        if( rawValues == null || !rawValues.hasMoreElements() )
        {
            return defaultValues;
        }

        // convert values to requested type
        Set<T> values = new LinkedHashSet<>();
        for( String rawValue : list( rawValues ) )
        {
            values.add( this.attributes.getConversionService().convert( HttpFields.valueParameters( rawValue, null ), type ) );
        }
        return values;
    }

    @Nonnull
    private <T> List<T> getOrderedHeaderValues( @Nonnull Class<T> type,
                                                @Nonnull HttpHeader header,
                                                @Nonnull List<T> defaultValues )
    {
        // parse all values for the header
        Enumeration<String> rawValues = this.request.getHttpFields().getValues( header.toString(), HttpFields.__separators );
        if( rawValues == null || !rawValues.hasMoreElements() )
        {
            return defaultValues;
        }

        // sort the list in quality order
        List<String> sortedRawValues = HttpFields.qualityList( rawValues );
        if( sortedRawValues.isEmpty() )
        {
            return defaultValues;
        }

        // convert values to requested type
        List<T> values = new LinkedList<>();
        for( String rawValue : sortedRawValues )
        {
            values.add( this.attributes.getConversionService().convert( HttpFields.valueParameters( rawValue, null ), type ) );
        }
        return values;
    }

    private class UriImpl implements Uri
    {
        @Nonnull
        private final String scheme;

        @Nonnull
        private final String host;

        private final int port;

        @Nonnull
        private final String decodedPath;

        @Nonnull
        private final String encodedPath;

        @Nonnull
        private final String encodedQueryString;

        @Nonnull
        private final Multimap<String, String> decodedQueryParameters;

        @Nonnull
        private final String fragment;

        @Nonnull
        private final LoadingCache<Pair<String, String>, MapEx<String, String>> pathTemplatesCache;

        private UriImpl( @Nonnull LoadingCache<Pair<String, String>, MapEx<String, String>> pathTemplatesCache )
        {
            this.pathTemplatesCache = pathTemplatesCache;
            this.scheme = WebRequestImpl.this.request.getScheme();
            this.host = WebRequestImpl.this.request.getServerName();
            this.port = WebRequestImpl.this.request.getServerPort();
            this.decodedPath = canonicalPath( Objects.toString( WebRequestImpl.this.request.getUri().getDecodedPath(), "/" ) );
            this.encodedPath = canonicalPath( Objects.toString( WebRequestImpl.this.request.getUri().getPath(), "/" ) );
            this.encodedQueryString = Objects.toString( WebRequestImpl.this.request.getUri().getQuery(), "" );
            this.decodedQueryParameters = extractDecodedQueryParameters();
            this.fragment = Objects.toString( WebRequestImpl.this.request.getUri().getFragment(), "" );
        }

        @Nonnull
        @Override
        public String getScheme()
        {
            return this.scheme;
        }

        @Nonnull
        @Override
        public String getHost()
        {
            return this.host;
        }

        @Override
        public int getPort()
        {
            return this.port;
        }

        @Nonnull
        @Override
        public String getDecodedPath()
        {
            return this.decodedPath;
        }

        @Nonnull
        @Override
        public String getEncodedPath()
        {
            return this.encodedPath;
        }

        @Nullable
        @Override
        public MapEx<String, String> getPathParameters( @Nonnull String pathTemplate )
        {
            ImmutablePair<String, String> key = ImmutablePair.of( pathTemplate, this.encodedPath );
            MapEx<String, String> params = this.pathTemplatesCache.getUnchecked( key );
            return params == NO_MATCH ? null : params;
        }

        @Nonnull
        @Override
        public Multimap<String, String> getDecodedQueryParameters()
        {
            return this.decodedQueryParameters;
        }

        @Nonnull
        @Override
        public String getEncodedQueryString()
        {
            return this.encodedQueryString;
        }

        @Nonnull
        @Override
        public String getFragment()
        {
            return this.fragment;
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder( 200 );
            buf.append( getScheme() ).append( "://" );
            buf.append( getHost() ).append( ":" ).append( getPort() );
            buf.append( getEncodedPath() );
            if( !this.encodedQueryString.isEmpty() )
            {
                buf.append( "?" ).append( getEncodedQueryString() );
            }
            if( !this.fragment.isEmpty() )
            {
                buf.append( "#" ).append( getFragment() );
            }
            return buf.toString();
        }
    }
}
