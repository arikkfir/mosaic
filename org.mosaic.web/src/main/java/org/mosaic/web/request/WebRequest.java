package org.mosaic.web.request;

import java.net.InetAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.application.Application;
import org.slf4j.Logger;

/**
 * @author arik
 */
public interface WebRequest
{
    @Nonnull
    Application getApplication();

    @Nonnull
    InetAddress getClientAddress();

    @Nonnull
    String getProtocol();

    @Nonnull
    String getMethod();

    @Nonnull
    WebRequestUri getUri();

    @Nonnull
    WebDevice getClientDevice();

    @Nonnull
    WebRequestHeaders getHeaders();

    @Nullable
    WebRequestCookie getCookie( String name );

    @Nonnull
    MapEx<String, Object> getAttributes();

    @Nullable
    WebSession getSession();

    @Nonnull
    WebSession getOrCreateSession();

    @Nullable
    Application.ApplicationSecurity.SecurityConstraint getSecurityConstraint();

    @Nonnull
    WebResponse getResponse();

    void dumpToTraceLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments );

    void dumpToDebugLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments );

    void dumpToInfoLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments );

    void dumpToWarnLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments );

    void dumpToErrorLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments );
}
