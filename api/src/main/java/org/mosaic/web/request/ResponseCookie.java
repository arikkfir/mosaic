package org.mosaic.web.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;

/**
 * @author arik
 */
public interface ResponseCookie extends RequestCookie
{
    void setValue( @Nullable String value );

    void setDomain( @Nonnull String domain );

    void setPath( @Nullable String path );

    void setMaxAge( @Nullable Period maxAge );

    void setSecure( boolean secure );

    void setComment( @Nullable String comment );

    void setHttpOnly( boolean httpOnly );

    void setVersion( int version );
}
