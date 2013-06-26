package org.mosaic.web.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;

/**
 * @author arik
 */
public interface RequestCookie
{
    @Nonnull
    String getName();

    @Nullable
    String getValue();

    @Nullable
    String getDomain();

    @Nullable
    String getPath();

    @Nullable
    Period getMaxAge();

    boolean isSecure();

    @Nullable
    String getComment();

    boolean isHttpOnly();

    int getVersion();
}
