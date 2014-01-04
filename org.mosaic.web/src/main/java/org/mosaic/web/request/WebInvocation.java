package org.mosaic.web.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Period;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.application.Application;
import org.mosaic.web.http.HttpRequest;
import org.mosaic.web.http.HttpResponse;
import org.mosaic.web.http.support.HttpLogger;
import org.mosaic.web.security.SecurityConstraint;

/**
 * @author arik
 */
public interface WebInvocation
{
    @Nonnull
    HttpRequest getHttpRequest();

    @Nonnull
    HttpResponse getHttpResponse();

    @Nonnull
    HttpLogger getHttpLogger();

    @Nonnull
    Application getApplication();

    @Nonnull
    UserAgent getUserAgent();

    @Nonnull
    MapEx<String, Object> getAttributes();

    @Nullable
    SecurityConstraint getSecurityConstraint();

    @Nullable
    WebSession getSession();

    @Nonnull
    WebSession getOrCreateSession();

    @Nullable
    WebCookie getCookie( String name );

    void addCookie( @Nonnull String name, @Nonnull Object value );

    void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain );

    void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain, @Nonnull String path );

    void addCookie( @Nonnull String name,
                    @Nonnull Object value,
                    @Nonnull String domain,
                    @Nonnull String path,
                    @Nonnull Period maxAge );

    void removeCookie( @Nonnull String name );

    void disableCaching();
}
