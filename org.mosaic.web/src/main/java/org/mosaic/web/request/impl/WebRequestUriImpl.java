package org.mosaic.web.request.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.server.Request;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.collections.UnmodifiableMapEx;
import org.mosaic.util.resource.AntPathMatcher;
import org.mosaic.web.request.WebRequestUri;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class WebRequestUriImpl implements WebRequestUri
{
    @Nonnull
    private final Request request;

    @Nonnull
    private final String decodedPath;

    @Nonnull
    private final String encodedPath;

    @Nonnull
    private final String fragment;

    @Nonnull
    private final Multimap<String, String> queryParameters;

    @Nonnull
    private final Map<String, MapEx<String, String>> pathParameters = new HashMap<>();

    WebRequestUriImpl( @Nonnull Request request )
    {
        this.request = request;
        this.decodedPath = this.request.getUri().getDecodedPath();
        this.encodedPath = this.request.getUri().getPath();

        String fragment = this.request.getUri().getFragment();
        this.fragment = fragment == null ? "" : fragment;

        Map<String, String[]> queryParametersMap = this.request.getParameterMap();
        Multimap<String, String> queryParameters = ArrayListMultimap.create();
        for( Map.Entry<String, String[]> entry : queryParametersMap.entrySet() )
        {
            queryParameters.putAll( entry.getKey(), asList( entry.getValue() ) );
        }
        this.queryParameters = queryParameters;
    }

    @Nonnull
    @Override
    public String getScheme()
    {
        return this.request.getScheme();
    }

    @Nonnull
    @Override
    public String getHost()
    {
        return this.request.getServerName();
    }

    @Override
    public int getPort()
    {
        return this.request.getServerPort();
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

        AntPathMatcher pathMatcher = new AntPathMatcher();
        if( pathMatcher.matches( pathTemplate, getEncodedPath(), pathParameters ) )
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
    public Multimap<String, String> getDecodedQueryParameters()
    {
        return this.queryParameters;
    }

    @Nonnull
    @Override
    public String getEncodedQueryString()
    {
        String queryString = this.request.getQueryString();
        return queryString == null ? "" : queryString;
    }

    @Nonnull
    @Override
    public String getFragment()
    {
        return this.fragment;
    }
}
