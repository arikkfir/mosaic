package org.mosaic.util.resource.impl;

import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.util.resource.PathMatcher;

/**
 * @author arik
 */
final class PathMatcherImpl implements PathMatcher
{
    @Nonnull
    private final OpenAntPathMatcher antPathMatcher = new OpenAntPathMatcher();

    @Override
    public boolean matches( @Nonnull String pattern, @Nonnull String path )
    {
        return this.antPathMatcher.match( pattern, path );
    }

    @Override
    public boolean matches( @Nonnull String pattern, @Nonnull String path, @Nonnull Map<String, String> pathParameters )
    {
        return this.antPathMatcher.doMatch( pattern, path, true, pathParameters );
    }

    private class OpenAntPathMatcher extends org.springframework.util.AntPathMatcher
    {
        @Override
        public boolean doMatch( String pattern,
                                String path,
                                boolean fullMatch,
                                Map<String, String> uriTemplateVariables )
        {
            return super.doMatch( pattern, path, fullMatch, uriTemplateVariables );
        }
    }
}
