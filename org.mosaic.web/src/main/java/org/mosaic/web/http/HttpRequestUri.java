package org.mosaic.web.http;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface HttpRequestUri
{
    @Nonnull
    String getScheme();

    @Nonnull
    String getHost();

    int getPort();

    @Nonnull
    String getDecodedPath();

    @Nonnull
    String getEncodedPath();

    @Nullable
    MapEx<String, String> getPathParameters( @Nonnull String pathTemplate );

    @Nonnull
    String getEncodedQueryString();

    @Nonnull
    String getFragment();
}
