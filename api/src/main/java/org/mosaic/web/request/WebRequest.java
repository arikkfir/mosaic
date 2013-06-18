package org.mosaic.web.request;

import com.google.common.collect.Multimap;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.User;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.net.HttpMethod;

/**
 * @author arik
 */
public interface WebRequest extends Map<String, Object>
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
    WebApplication getApplication();

    @Nonnull
    User getUser();

    @Nullable
    WebSession getSession();

    @Nonnull
    WebSession getOrCreateSession();

    @Nonnull
    InetAddress getClientAddress();

    @Nonnull
    Map<String, String> getPathParameters();

    @Nonnull
    WebRequestHeaders getHeaders();

    @Nonnull
    ByteBuffer getBytesContents();

    @Nonnull
    CharBuffer getCharsContents();

    @Nullable
    WebPart getPart( @Nonnull String name );

    @Nonnull
    Map<String, WebPart> getPartsMap();

    @Nonnull
    WebResponse getResponse();

    void dumpToLog( @Nullable String message, @Nullable Object... arguments );
}
