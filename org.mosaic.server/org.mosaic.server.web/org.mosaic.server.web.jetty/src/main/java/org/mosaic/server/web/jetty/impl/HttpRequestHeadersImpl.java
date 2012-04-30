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
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.util.collection.MissingRequiredValueException;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
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
public class HttpRequestHeadersImpl implements HttpRequestHeaders {

    private static final Logger LOG = LoggerFactory.getLogger( HttpRequestHeadersImpl.class );

    private static final Pattern QUALITY_PATTERN = Pattern.compile( "q=(?<quality>\\d+(?:\\.\\d+))" );

    private static final Comparator<MediaType> MEDIA_TYPE_COMPARATOR = reverseOrder( new MediaTypeComparator() );

    private static final Comparator<OrderedEntry<Charset>> CHARSET_COMPARATOR = reverseOrder( new OrderedEntryComparator<Charset>() );

    private static final Set<String> SINGLE_VALUES_HEADERS = new HashSet<>( Arrays.<String>asList(
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

    private static boolean isMosaicOverrideHeader( String header ) {
        return header.startsWith( "X-Mosaic-" ) && header.endsWith( "-Override" );
    }

    private static boolean isSingleValuedHeader( String header ) {
        return SINGLE_VALUES_HEADERS.contains( header );
    }

    private final HttpServletRequest request;

    private final TypedDict<String> headers;

    public HttpRequestHeadersImpl( HttpServletRequest request, ConversionService conversionService ) {
        this.request = request;

        // copy all headers to our map
        this.headers = new WrappingTypedDict<>( new HashMap<String, List<String>>( 20 ), conversionService, String.class );
        for( String header : list( this.request.getHeaderNames() ) ) {

            // override headers do not stand by themselves - their values must be used under the overridden header name
            if( isMosaicOverrideHeader( header ) ) {
                continue;
            }

            // first check if this header was overridden by a Mosaic Override header
            List<String> values = list( this.request.getHeaders( "X-" + header + "-Override" ) );
            if( values == null || values.isEmpty() ) {
                // not overridden - use this header's value
                values = list( this.request.getHeaders( header ) );
            }

            // add header to the map
            for( String value : values ) {

                // single-valued headers are those that might appear multiple times in the request, but their values
                // must be joined to a single value separated by commas (",")
                if( isSingleValuedHeader( header ) && this.headers.containsKey( header ) ) {
                    String oldValue = this.headers.getValue( header );
                    if( oldValue.trim().length() > 0 ) {
                        this.headers.put( header, oldValue + ", " + value );
                    } else {
                        this.headers.put( header, value );
                    }
                } else {
                    this.headers.add( header, value );
                }
            }

        }
    }

    @Override
    public List<MediaType> getAccept() {
        List<MediaType> mediaTypes = parseMediaTypes( getValue( "Accept", MediaType.ALL.toString() ) );
        Collections.sort( mediaTypes, MEDIA_TYPE_COMPARATOR );
        return mediaTypes;
    }

    @Override
    public List<Charset> getAcceptCharset() {
        String values = getValue( "Accept-Charset", "ISO-8859-1" );

        List<OrderedEntry<Charset>> orderedCharSets = new ArrayList<>( 5 );
        for( String token : values.split( ",\\s*" ) ) {
            int paramIdx = token.indexOf( ';' );
            try {
                if( paramIdx == -1 ) {
                    if( !"*".equals( token.trim() ) ) {
                        orderedCharSets.add( new OrderedEntry<>( Charset.forName( token ) ) );
                    }
                } else {
                    String name = token.substring( 0, paramIdx );
                    if( !"*".equals( name.trim() ) ) {
                        String extra = token.substring( paramIdx + 1 );
                        Matcher qualityMatcher = QUALITY_PATTERN.matcher( extra );
                        if( qualityMatcher.matches() ) {
                            orderedCharSets.add( new OrderedEntry<>(
                                    Double.valueOf( qualityMatcher.group( "quality" ) ),
                                    Charset.forName( token.substring( paramIdx + 1 ) ) ) );
                        } else {
                            orderedCharSets.add( new OrderedEntry<>(
                                    Charset.forName( token.substring( paramIdx + 1 ) ) ) );
                        }
                    }
                }
            } catch( IllegalCharsetNameException e ) {
                LOG.debug( "Illegal charset name encountered: {}", token );
            } catch( UnsupportedCharsetException e ) {
                LOG.debug( "Unsupported charset encountered: {}", token );
            }
        }

        Collections.sort( orderedCharSets, CHARSET_COMPARATOR );
        List<Charset> charSets = new LinkedList<>();
        for( OrderedEntry<Charset> entry : orderedCharSets ) {
            charSets.add( entry.value );
        }
        return charSets;
    }

    @Override
    public List<Locale> getAcceptLanguage() {
        // use the request object directly because it knows to parse the Accept-Language header, and sort
        // the languages according to the sent quality value
        return Collections.list( this.request.getLocales() );
    }

    @Override
    public String getAuthorization() {
        return getValue( "Authorization" );
    }

    @Override
    public String getCacheControl() {
        return getValue( "Cache-Control" );
    }

    @Override
    public List<Locale> getContentLanguage() {
        List<Locale> languages = new LinkedList<>();
        for( String language : getValue( "Content-Language", "en" ).split( ",\\s*" ) ) {
            languages.add( Locale.forLanguageTag( language ) );
        }
        return languages;
    }

    @Override
    public Long getContentLength() {
        return getValueAs( "Content-Length", Long.class );
    }

    @Override
    public MediaType getContentType() {
        return getValueAs( "Content-Type", MediaType.class );
    }

    @Override
    public HttpCookie getCookie( String name ) {

        // use the request object directly since it knows the parse cookies better than we do :)
        Cookie[] cookies = this.request.getCookies();
        if( cookies != null ) {
            for( Cookie cookie : cookies ) {
                if( cookie.getName().equalsIgnoreCase( name ) ) {
                    return new HttpCookieImpl( cookie );
                }
            }
        }
        return null;

    }

    @Override
    public String getHost() {
        return getValue( "Host" );
    }

    @Override
    public Set<String> getIfMatch() {
        Set<String> eTags = new LinkedHashSet<>( 5 );
        String value = getValue( "If-Match" );
        if( value != null ) {
            Collections.addAll( eTags, value.split( ",\\s*" ) );
        }
        return eTags;
    }

    @Override
    public DateTime getIfModifiedSince() {
        return getValueAs( "If-Modified-Since", DateTime.class );
    }

    @Override
    public Set<String> getIfNoneMatch() {
        Set<String> eTags = new LinkedHashSet<>( 5 );
        String value = getValue( "If-None-Match" );
        if( value != null ) {
            Collections.addAll( eTags, value.split( ",\\s*" ) );
        }
        return eTags;
    }

    @Override
    public DateTime getIfUnmodifiedSince() {
        return getValueAs( "If-Unmodified-Since", DateTime.class );
    }

    @Override
    public String getPragma() {
        return getValue( "Pragma" );
    }

    @Override
    public URL getReferer() {
        String value = getValue( "Referer" );
        if( value == null ) {
            return null;
        }

        String requestURL = this.request.getRequestURL().toString();

        // create a URI for the referer header
        URI referrer;
        try {
            referrer = new URI( value );
        } catch( URISyntaxException e ) {
            LOG.debug( "Illegal 'Referer' header sent to '{}': {}", requestURL, e.getMessage(), e );
            return null;
        }
        if( referrer.getScheme() == null ) {

            // relative referer - resolve against this request's URL
            try {
                return URI.create( requestURL ).resolve( referrer ).toURL();
            } catch( MalformedURLException e ) {
                LOG.warn( "Could not resolve 'Referer' header value '{}' against base request URI '{}': {}",
                          referrer, requestURL, e.getMessage(), e );
                return null;
            }

        } else {

            // absolute referer - just return that
            try {
                return referrer.toURL();
            } catch( MalformedURLException e ) {
                LOG.warn( "Could not convert 'Referer' header value '{}' for request '{}' into a URL object: {}",
                          referrer, requestURL, e.getMessage(), e );
                return null;
            }
        }
    }

    @Override
    public String getUserAgent() {
        return getValue( "User-Agent" );
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
        T converted = getValueAs( key, type );
        if( converted == null ) {
            throw new MissingRequiredValueException( key );
        } else {
            return converted;
        }
    }

    @Override
    public void add( String key, String value ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public <T> void addAs( String key, T value ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public <T> void putAs( String key, T value ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public void put( String key, String value ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
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
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public List<String> remove( Object key ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public void putAll( Map<? extends String, ? extends List<String>> m ) {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException( "Request headers cannot be modified! (perhaps you intended to modify the \"HttpRequest.getResponseHeaders()\" instead?)" );
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

    private static class MediaTypeComparator implements Comparator<MediaType> {

        @Override
        public int compare( MediaType o1, MediaType o2 ) {
            if( o1 == o2 ) {
                return 0;
            }

            double q1 = o1.getQualityValue();
            double q2 = o2.getQualityValue();
            if( q1 == q2 ) {
                return 0;
            } else if( q1 < q2 ) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private class OrderedEntry<T> {

        private final double order;

        private final T value;

        private OrderedEntry( T value ) {
            this.order = 1;
            this.value = value;
        }

        private OrderedEntry( double order, T value ) {
            this.order = order;
            this.value = value;
        }
    }

    private static class OrderedEntryComparator<T> implements Comparator<OrderedEntry<T>> {

        @Override
        public int compare( OrderedEntry<T> o1, OrderedEntry<T> o2 ) {
            if( o1 == o2 || o1.order == o2.order ) {
                return 0;
            } else if( o1.order < o2.order ) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
