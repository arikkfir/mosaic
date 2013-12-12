package org.mosaic.web.request.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import org.joda.time.Period;
import org.mosaic.web.request.WebRequestCookie;

/**
 * @author arik
 */
final class WebRequestCookieImpl implements WebRequestCookie
{
    @Nonnull
    private final Cookie cookie;

    WebRequestCookieImpl( @Nonnull Cookie cookie )
    {
        this.cookie = cookie;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.cookie.getName();
    }

    @Nullable
    @Override
    public String getValue()
    {
        return this.cookie.getValue();
    }

    @Nullable
    @Override
    public String getDomain()
    {
        return this.cookie.getDomain();
    }

    @Nullable
    @Override
    public String getPath()
    {
        return this.cookie.getPath();
    }

    @Nullable
    @Override
    public Period getMaxAge()
    {
        return Period.seconds( this.cookie.getMaxAge() );
    }

    @Override
    public boolean isSecure()
    {
        return this.cookie.getSecure();
    }

    @Nullable
    @Override
    public String getComment()
    {
        return this.cookie.getComment();
    }

    @Override
    public boolean isHttpOnly()
    {
        return this.cookie.isHttpOnly();
    }

    @Override
    public int getVersion()
    {
        return this.cookie.getVersion();
    }
}
