package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface WebRequestUri
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
    Multimap<String, String> getDecodedQueryParameters();

    @Nonnull
    String getEncodedQueryString();

    @Nonnull
    String getFragment();
}
