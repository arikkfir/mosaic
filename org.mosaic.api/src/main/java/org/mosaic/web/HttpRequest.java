package org.mosaic.web;

import java.io.*;
import java.net.URI;
import java.util.Collection;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.collection.MultiMapAccessor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * @author arik
 */
public interface HttpRequest extends MapAccessor<String, Object>
{
    HttpApplication getApplication();

    HttpSession getSession();

    HttpSession getOrCreateSession();

    boolean isSecure();

    String getClientAddress();

    String getProtocol();

    HttpMethod getMethod();

    URI getUrl();

    MultiMapAccessor<String, String> getQueryParameters();

    MapAccessor<String, String> getPathParameters();

    HttpRequestHeaders getRequestHeaders();

    InputStream getRequestInputStream() throws IOException;

    Reader getRequestReader() throws IOException;

    HttpPart getPart( String name );

    Collection<HttpPart> getParts();

    HttpStatus getResponseStatus();

    void setResponseStatus( HttpStatus status, String text );

    HttpResponseHeaders getResponseHeaders();

    OutputStream getResponseOutputStream() throws IOException;

    Writer getResponseWriter() throws IOException;

    boolean isCommitted();
}
