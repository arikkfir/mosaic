package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.mosaic.web.net.HttpMethod;

/**
 * @author arik
 */
public interface Url
{
    @Nonnull
    HttpMethod getMethod();

    @Nonnull
    String getScheme();

    @Nonnull
    String getHost();

    int getPort();

    @Nonnull
    String getDecodedPath();

    @Nonnull
    String getEncodedPath();

    @Nonnull
    String getEncodedQueryString();

    @Nonnull
    Multimap<String, String> getDecodedQueryParameters();

    @Nonnull
    String toString();

    boolean isSecure();
}
