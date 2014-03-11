package org.mosaic.web.server.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import org.eclipse.jetty.server.Request;
import org.joda.time.DateTime;
import org.mosaic.modules.Service;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.collections.UnmodifiableMapEx;
import org.mosaic.util.resource.PathMatcher;
import org.mosaic.web.server.HttpRequest;
import org.mosaic.web.server.HttpRequestUri;

import static com.google.common.net.HttpHeaders.*;

/**
 * @author arik
 */
final class JettyHttpRequestImpl implements HttpRequest
{
    private static final Charset UTF_8 = Charset.forName( "UTF-8" );

    @Nonnull
    private final Request request;

    @Nonnull
    private final String httpMethod;

    @Nonnull
    private final HttpRequestUri uri;

    @Nonnull
    private final Headers headers;

    @Nonnull
    @Service
    private PathMatcher pathMatcher;

    JettyHttpRequestImpl( @Nonnull Request request )
    {
        this.request = request;
        this.httpMethod = this.request.getMethod().toLowerCase();
        this.uri = new HttpRequestUriImpl();
        this.headers = new Headers( this.request.getHttpFields() );
    }

    @Nonnull
    public Request getJettyRequest()
    {
        return this.request;
    }

    @Nonnull
    @Override
    public String getClientAddress()
    {
        return this.request.getRemoteAddr();
    }

    @Nonnull
    @Override
    public String getMethod()
    {
        return this.httpMethod;
    }

    @Nonnull
    @Override
    public HttpRequestUri getUri()
    {
        return this.uri;
    }

    @Nonnull
    @Override
    public String getProtocol()
    {
        return this.request.getProtocol();
    }

    @Nonnull
    @Override
    public List<MediaType> getAccept()
    {
        return this.headers.getMediaTypes( ACCEPT );
    }

    @Nonnull
    @Override
    public List<Charset> getAccepCharset()
    {
        return this.headers.getCharsets( ACCEPT_CHARSET );
    }

    @Nonnull
    @Override
    public List<String> getAcceptEncoding()
    {
        return this.headers.getStrings( ACCEPT_ENCODING );
    }

    @Nonnull
    @Override
    public List<Locale> getAcceptLanguage()
    {
        return this.headers.getLocales( ACCEPT_LANGUAGE );
    }

    @Nonnull
    @Override
    public List<String> getAllow()
    {
        return headers.getStrings( ALLOW );
    }

    @Nullable
    @Override
    public String getAuthorization()
    {
        return this.headers.getString( AUTHORIZATION );
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return this.headers.getString( CACHE_CONTROL );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return this.headers.getString( CONNECTION );
    }

    @Nonnull
    @Override
    public List<String> getContentEncoding()
    {
        return headers.getStrings( CONTENT_ENCODING );
    }

    @Nullable
    @Override
    public Locale getContentLanguage()
    {
        return headers.getLocale( CONTENT_LANGUAGE );
    }

    @Nonnull
    @Override
    public List<Locale> getContentLanguages()
    {
        return headers.getLocales( CONTENT_LANGUAGE );
    }

    @Nullable
    @Override
    public Long getContentLength()
    {
        return headers.getLong( CONTENT_LENGTH );
    }

    @Nullable
    @Override
    public String getContentLocation()
    {
        return headers.getString( CONTENT_LOCATION );
    }

    @Nullable
    @Override
    public String getContentMd5()
    {
        return headers.getString( CONTENT_MD5 );
    }

    @Nullable
    @Override
    public String getContentRange()
    {
        return headers.getString( CONTENT_RANGE );
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        return headers.getMediaType( CONTENT_TYPE );
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        return this.headers.getDateTime( DATE );
    }

    @Nullable
    @Override
    public String getExpect()
    {
        return this.headers.getString( EXPECT );
    }

    @Nullable
    @Override
    public String getFrom()
    {
        return this.headers.getString( FROM );
    }

    @Nonnull
    @Override
    public String getHost()
    {
        return this.request.getServerName();
    }

    @Nonnull
    @Override
    public List<String> getIfMatch()
    {
        return this.headers.getStrings( IF_MATCH );
    }

    @Nullable
    @Override
    public DateTime getIfModifiedSince()
    {
        return this.headers.getDateTime( IF_MODIFIED_SINCE );
    }

    @Nonnull
    @Override
    public List<String> getIfNoneMatch()
    {
        return this.headers.getStrings( IF_NONE_MATCH );
    }

    @Nullable
    @Override
    public DateTime getIfRangeDate()
    {
        String value = this.headers.getString( IF_RANGE );
        return value != null && value.contains( "GMT" ) ? this.headers.getDateTime( IF_RANGE ) : null;
    }

    @Nullable
    @Override
    public String getIfRangeETag()
    {
        String value = this.headers.getString( IF_RANGE );
        return value != null && !value.contains( "GMT" ) ? this.headers.getString( IF_RANGE ) : null;
    }

    @Nullable
    @Override
    public DateTime getIfUnmodifiedSince()
    {
        return this.headers.getDateTime( IF_UNMODIFIED_SINCE );
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return this.headers.getString( PRAGMA );
    }

    @Nonnull
    @Override
    public List<String> getRange()
    {
        return this.headers.getStrings( RANGE );
    }

    @Nullable
    @Override
    public String getReferer()
    {
        return this.headers.getString( REFERER );
    }

    @Nullable
    @Override
    public String getUserAgent()
    {
        return this.headers.getString( USER_AGENT );
    }

    @Nullable
    @Override
    public String getVia()
    {
        return this.headers.getString( VIA );
    }

    @Nullable
    @Override
    public String getWarning()
    {
        return this.headers.getString( WARNING );
    }

    @Override
    public int getHeadersCount()
    {
        return this.headers.size();
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
    public Set<String> getHeaderNames()
    {
        return this.headers.keySet();
    }

    @Nonnull
    @Override
    public List<String> getHeader( @Nonnull String key )
    {
        return this.headers.getStrings( key );
    }

    @Nonnull
    @Override
    public InputStream stream() throws IOException
    {
        return request.getInputStream();
    }

    @Nonnull
    @Override
    public Reader reader() throws IOException
    {
        return request.getReader();
    }

    @Nonnull
    @Override
    public <T> T unmarshall( @Nonnull Class<T> type )
    {
        // TODO arik: implement org.mosaic.web.http.impl.HttpRequestImpl.HttpRequestEntityImpl.unmarshall([type])
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public HttpRequestPart getPart( @Nonnull String name ) throws IOException
    {
        try
        {
            Part part = this.request.getPart( name );
            return part == null ? null : new HttpRequestPartImpl( part );
        }
        catch( ServletException e )
        {
            throw new IllegalStateException( "request is not multipart", e );
        }
    }

    @Nonnull
    @Override
    public Map<String, ? extends HttpRequestPart> getParts() throws IOException
    {
        try
        {
            Collection<Part> realParts = this.request.getParts();
            Map<String, HttpRequestPartImpl> parts = new HashMap<>( realParts.size() );
            for( Part realPart : realParts )
            {
                parts.put( realPart.getName(), new HttpRequestPartImpl( realPart ) );
            }
            return parts;
        }
        catch( ServletException e )
        {
            throw new IllegalStateException( "request is not multipart", e );
        }
    }

    private class HttpRequestUriImpl implements HttpRequestUri
    {
        @Nonnull
        private final String decodedPath;

        @Nonnull
        private final String encodedPath;

        @Nonnull
        private final String encodedQuery;

        @Nonnull
        private final String fragment;

        @Nonnull
        private final Map<String, MapEx<String, String>> pathParameters = new HashMap<>();

        HttpRequestUriImpl()
        {
            this.decodedPath = request.getUri().getDecodedPath();
            this.encodedPath = request.getUri().getPath();

            String queryString = request.getQueryString();
            this.encodedQuery = queryString == null ? "" : queryString;

            String fragment = request.getUri().getFragment();
            this.fragment = fragment == null ? "" : fragment;
        }

        @Nonnull
        @Override
        public String getScheme()
        {
            return request.getScheme();
        }

        @Nonnull
        @Override
        public String getHost()
        {
            return request.getServerName();
        }

        @Override
        public int getPort()
        {
            return request.getServerPort();
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
            if( this.pathParameters.containsKey( pathTemplate ) )
            {
                return this.pathParameters.get( pathTemplate );
            }

            MapEx<String, String> pathParameters = new HashMapEx<>();

            if( JettyHttpRequestImpl.this.pathMatcher.matches( pathTemplate, getEncodedPath(), pathParameters ) )
            {
                pathParameters = UnmodifiableMapEx.of( pathParameters );
                this.pathParameters.put( pathTemplate, pathParameters );
                return pathParameters;
            }
            else
            {
                return null;
            }
        }

        @Nonnull
        @Override
        public String getEncodedQueryString()
        {
            return this.encodedQuery;
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
            return this.encodedPath + ( this.encodedQuery.isEmpty() ? "" : "?" + this.encodedQuery ) + this.fragment;
        }
    }

    private class HttpRequestPartImpl implements HttpRequestPart
    {
        @Nonnull
        private final Part part;

        private HttpRequestPartImpl( @Nonnull Part part )
        {
            this.part = part;
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.part.getName();
        }

        @Nullable
        @Override
        public Long getContentLength()
        {
            long size = this.part.getSize();
            return size < 0 ? null : size;
        }

        @Nullable
        @Override
        public MediaType getContentType()
        {
            String contentType = this.part.getContentType();
            return contentType == null ? null : MediaType.parse( contentType );
        }

        @Nonnull
        @Override
        public InputStream stream() throws IOException
        {
            return this.part.getInputStream();
        }

        @Nonnull
        @Override
        public Reader reader() throws IOException
        {
            MediaType contentType = getContentType();
            Charset encoding = contentType == null ? UTF_8 : contentType.charset().or( UTF_8 );
            return new InputStreamReader( this.part.getInputStream(), encoding );
        }

        @Nonnull
        @Override
        public <T> T unmarshall( @Nonnull Class<T> type )
        {
            // TODO arik: implement org.mosaic.web.http.impl.HttpRequestImpl.HttpRequestPartImpl.unmarshall([type])
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveLocally( @Nonnull Path file ) throws IOException
        {
            this.part.write( file.toString() );
        }

        @Nonnull
        @Override
        public Path saveToTempFile() throws IOException
        {
            Path tempFile = Files.createTempFile( "mosaic-webpart-", ".tmp" );
            saveLocally( tempFile );
            return tempFile;
        }
    }
}
