package org.mosaic.util.resource;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface PathMatcher
{
    boolean matches( @Nonnull String pattern, @Nonnull String path );

    boolean matches( @Nonnull String pattern, @Nonnull String path, @Nonnull Map<String, String> pathParameters );
}
