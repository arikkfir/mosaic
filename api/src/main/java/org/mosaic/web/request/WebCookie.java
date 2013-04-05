package org.mosaic.web.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface WebCookie
{
    @Nonnull
    String getName();

    @Nullable
    String getValue();

    void setValue( @Nullable String value );

    @Nullable
    String getDomain();

    void setDomain( @Nonnull String domain );

    @Nullable
    String getPath();

    void setPath( @Nullable String path );

    @Nullable
    Integer getMaxAgeInSeconds();

    void setMaxAgeInSeconds( @Nullable Integer maxAgeInSeconds );

    boolean isSecure();

    void setSecure( boolean secure );

    @Nullable
    String getComment();

    void setComment( @Nullable String comment );

    boolean isHttpOnly();

    void setHttpOnly( boolean httpOnly );

    int getVersion();

    void setVersion( int version );
}
