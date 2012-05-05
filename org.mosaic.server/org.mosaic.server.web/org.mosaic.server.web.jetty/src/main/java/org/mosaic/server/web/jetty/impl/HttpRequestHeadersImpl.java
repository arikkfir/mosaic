package org.mosaic.server.web.jetty.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.mosaic.util.collection.MultiMapAccessor;
import org.mosaic.util.collection.MultiMapWrapper;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpCookie;
import org.mosaic.web.HttpRequestHeaders;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;

import static java.util.Collections.list;
import static java.util.Collections.reverseOrder;
import static org.springframework.http.MediaType.parseMediaTypes;

/**
 * @author arik
 */
public class HttpRequestHeadersImpl implements HttpRequestHeaders
{
    private static final Logger LOG = LoggerFactory.getLogger( HttpRequestHeadersImpl.class );

    private static final Pattern QUALITY_PATTERN = Pattern.compile( "q=(?<quality>\\d+(?:\\.\\d+))" );

    private static final Comparator<OrderedEntry<Charset>> CHARSET_COMPARATOR = reverseOrder( new OrderedEntryComparator<Charset>() );

    private static final Set<String> SINGLE_VALUES_HEADERS =
            new HashSet<>( Arrays.<String>asList(
                    "Accept",
                    "Accept-Charset",
                    "Accept-Encoding",
                    "Accept-Language",
                    "Allow",
                    "Content-Language",
                    "If-Match",
                    "If-None-Match",
                    "X-Mosaic-Accept-Override",
                    "X-Mosaic-Accept-Charset-Override",
                    "X-Mosaic-Accept-Encoding-Override",
                    "X-Mosaic-Accept-Language-Override",
                    "X-Mosaic-Allow-Override",
                    "X-Mosaic-Content-Language-Override",
                    "X-Mosaic-If-Match-Override",
                    "X-Mosaic-If-None-Match-Override"
            ) );

    private final MultiMapWrapper<String, String> headers;

    private final ArrayList<Locale> locales;

    private final Cookie[] cookies;

    private final URL referer;

    public HttpRequestHeadersImpl( HttpServletRequest request, ConversionService conversionService )
    {
        // use the request object directly because it knows to parse the Accept-Language header, and sort
        // the languages according to the sent quality value
        this.locales = Collections.list( request.getLocales() );

        // get cookies
        this.cookies = request.getCookies();

        Map<String, List<String>> headers = new HashMap<>();
        MultiMapAccessor<String, String> hs = new MultiMapWrapper<>( headers );
        for( String header : list( request.getHeaderNames() ) )
        {
            // override headers do not stand by themselves - their values must be used under the overridden header name
            // TODO: what if the original header is not sent at all? (e.g. only "X-MyHeader-Override" is sent...)
            if( header.startsWith( "X-Mosaic-" ) && header.endsWith( "-Override" ) )
            {
                continue;
            }

            // first check if this header was overridden by a Mosaic Override header
            List<String> values = list( request.getHeaders( "X-" + header + "-Override" ) );
            if( values == null || values.isEmpty() )
            {
                // not overridden - use this header's value
                values = list( request.getHeaders( header ) );
            }

            // add header to the map
            for( String value : values )
            {
                // single-valued headers are those that might appear multiple times in the request, but their values
                // must be joined to a single value separated by commas (",")
                if( SINGLE_VALUES_HEADERS.contains( header ) && hs.containsKey( header ) )
                {
                    String oldValue = hs.getFirst( header );
                    if( oldValue.trim().length() > 0 )
                    {
                        hs.replace( header, oldValue + ", " + value );
                    }
                    else
                    {
                        hs.replace( header, value );
                    }
                }
                else
                {
                    hs.add( header, value );
                }
            }
        }
        this.headers = new MultiMapWrapper<>( Collections.unmodifiableMap( headers ), conversionService );
        this.referer = buildReferer( request );
    }

    @Override
    public List<MediaType> getAccept()
    {
        List<MediaType> mediaTypes = parseMediaTypes( getFirst( "Accept", MediaType.ALL.toString() ) );
        MediaType.sortByQualityValue( mediaTypes );
        return mediaTypes;
    }

    @Override
    public List<Charset> getAcceptCharset()
    {
        String values = getFirst( "Accept-Charset", "ISO-8859-1" );

        List<OrderedEntry<Charset>> orderedCharSets = new ArrayList<>( 5 );
        for( String token : values.split( ",\\s*" ) )
        {
            int paramIdx = token.indexOf( ';' );
            try
            {
                if( paramIdx == -1 )
                {
                    if( !"*".equals( token.trim() ) )
                    {
                        orderedCharSets.add( new OrderedEntry<>( Charset.forName( token ) ) );
                    }
                }
                else
                {
                    String name = token.substring( 0, paramIdx );
                    if( !"*".equals( name.trim() ) )
                    {
                        String extra = token.substring( paramIdx + 1 );
                        Matcher qualityMatcher = QUALITY_PATTERN.matcher( extra );
                        if( qualityMatcher.matches() )
                        {
                            orderedCharSets.add( new OrderedEntry<>(
                                    Double.valueOf( qualityMatcher.group( "quality" ) ),
                                    Charset.forName( token.substring( paramIdx + 1 ) ) )
                            );
                        }
                        else
                        {
                            orderedCharSets.add( new OrderedEntry<>(
                                    Charset.forName( token.substring( paramIdx + 1 ) ) )
                            );
                        }
                    }
                }
            }
            catch( IllegalCharsetNameException e )
            {
                LOG.debug( "Illegal charset name encountered: {}", token );
            }
            catch( UnsupportedCharsetException e )
            {
                LOG.debug( "Unsupported charset encountered: {}", token );
            }
        }

        Collections.sort( orderedCharSets, CHARSET_COMPARATOR );
        List<Charset> charSets = new LinkedList<>();
        for( OrderedEntry<Charset> entry : orderedCharSets )
        {
            charSets.add( entry.value );
        }
        return charSets;
    }

    @Override
    public List<Locale> getAcceptLanguage()
    {
        return this.locales;
    }

    @Override
    public String getAuthorization()
    {
        return getFirst( "Authorization" );
    }

    @Override
    public String getCacheControl()
    {
        return getFirst( "Cache-Control" );
    }

    @Override
    public List<Locale> getContentLanguage()
    {
        List<Locale> languages = new LinkedList<>();
        for( String language : getFirst( "Content-Language", "en" ).split( ",\\s*" ) )
        {
            languages.add( Locale.forLanguageTag( language ) );
        }
        // TODO by arik on 5/5/12: sort by quality
        return languages;
    }

    @Override
    public Long getContentLength()
    {
        return getFirst( "Content-Length", Long.class );
    }

    @Override
    public MediaType getContentType()
    {
        return MediaType.parseMediaType( getFirst( "Content-Type" ) );
    }

    @Override
    public HttpCookie getCookie( String name )
    {
        // use the request object directly since it knows the parse cookies better than we do :)
        if( cookies != null )
        {
            for( Cookie cookie : cookies )
            {
                if( cookie.getName().equalsIgnoreCase( name ) )
                {
                    return new HttpCookieImpl( cookie );
                }
            }
        }
        return null;

    }

    @Override
    public String getHost()
    {
        return getFirst( "Host" );
    }

    @Override
    public Set<String> getIfMatch()
    {
        Set<String> eTags = new LinkedHashSet<>( 5 );
        String value = getFirst( "If-Match" );
        if( value != null )
        {
            Collections.addAll( eTags, value.split( ",\\s*" ) );
        }
        return eTags;
    }

    @Override
    public DateTime getIfModifiedSince()
    {
        return getFirst( "If-Modified-Since", DateTime.class );
    }

    @Override
    public Set<String> getIfNoneMatch()
    {
        Set<String> eTags = new LinkedHashSet<>( 5 );
        String value = getFirst( "If-None-Match" );
        if( value != null )
        {
            Collections.addAll( eTags, value.split( ",\\s*" ) );
        }
        return eTags;
    }

    @Override
    public DateTime getIfUnmodifiedSince()
    {
        return getFirst( "If-Unmodified-Since", DateTime.class );
    }

    @Override
    public String getPragma()
    {
        return getFirst( "Pragma" );
    }

    @Override
    public URL getReferer()
    {
        return this.referer;
    }

    @Override
    public String getUserAgent()
    {
        return getFirst( "User-Agent" );
    }

    @Override
    public int size()
    {
        return headers.size();
    }

    @Override
    public boolean isEmpty()
    {
        return headers.isEmpty();
    }

    @Override
    public boolean containsKey( Object key )
    {
        return headers.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return headers.containsValue( value );
    }

    @Override
    public List<String> get( Object key )
    {
        return headers.get( key );
    }

    @Override
    public List<String> put( String key, List<String> value )
    {
        return headers.put( key, value );
    }

    @Override
    public List<String> replace( String key, String value )
    {
        return headers.replace( key, value );
    }

    @Override
    public List<String> remove( Object key )
    {
        return headers.remove( key );
    }

    @Override
    public void putAll( Map<? extends String, ? extends List<String>> m )
    {
        headers.putAll( m );
    }

    @Override
    public void clear()
    {
        headers.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return headers.keySet();
    }

    @Override
    public Collection<List<String>> values()
    {
        return headers.values();
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet()
    {
        return headers.entrySet();
    }

    @Override
    public String getFirst( String key )
    {
        return headers.getFirst( key );
    }

    @Override
    public String getFirst( String key, String defaultValue )
    {
        return headers.getFirst( key, defaultValue );
    }

    @Override
    public String requireFirst( String key )
    {
        return headers.requireFirst( key );
    }

    @Override
    public <T> T getFirst( String key, Class<T> type )
    {
        return headers.getFirst( key, type );
    }

    @Override
    public <T> T requireFirst( String key, Class<T> type )
    {
        return headers.requireFirst( key, type );
    }

    @Override
    public <T> T getFirst( String key, Class<T> type, T defaultValue )
    {
        return headers.getFirst( key, type, defaultValue );
    }

    @Override
    public void add( String key, String value )
    {
        headers.add( key, value );
    }

    private URL buildReferer( HttpServletRequest request )
    {
        String value = getFirst( "Referer" );
        if( value == null )
        {
            return null;
        }

        String requestURL = request.getRequestURL().toString();

        // create a URI for the referer header
        URI referrer;
        try
        {
            referrer = new URI( value );
        }
        catch( URISyntaxException e )
        {
            LOG.debug( "Illegal 'Referer' header sent to '{}': {}", requestURL, e.getMessage(), e );
            return null;
        }
        if( referrer.getScheme() == null )
        {
            // relative referer - resolve against this request URL
            try
            {
                return URI.create( requestURL ).resolve( referrer ).toURL();
            }
            catch( MalformedURLException e )
            {
                LOG.warn( "Could not resolve 'Referer' header value '{}' against base request URI '{}': {}", referrer, requestURL, e.getMessage(), e );
                return null;
            }
        }
        else
        {
            // absolute referer - just return that
            try
            {
                return referrer.toURL();
            }
            catch( MalformedURLException e )
            {
                LOG.warn( "Could not convert 'Referer' header value '{}' for request '{}' into a URL object: {}", referrer, requestURL, e.getMessage(), e );
                return null;
            }
        }
    }

    private class OrderedEntry<T>
    {

        private final double order;

        private final T value;

        private OrderedEntry( T value )
        {
            this.order = 1;
            this.value = value;
        }

        private OrderedEntry( double order, T value )
        {
            this.order = order;
            this.value = value;
        }
    }

    private static class OrderedEntryComparator<T> implements Comparator<OrderedEntry<T>>
    {

        @Override
        public int compare( OrderedEntry<T> o1, OrderedEntry<T> o2 )
        {
            if( o1 == o2 || o1.order == o2.order )
            {
                return 0;
            }
            else if( o1.order < o2.order )
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }
}
