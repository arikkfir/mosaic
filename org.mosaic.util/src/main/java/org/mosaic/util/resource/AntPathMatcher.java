package org.mosaic.util.resource;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class AntPathMatcher implements PathMatcher
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
