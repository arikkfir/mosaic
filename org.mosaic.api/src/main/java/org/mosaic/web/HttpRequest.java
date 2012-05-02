package org.mosaic.web;

import java.io.*;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * @author arik
 */
public interface HttpRequest extends Map<String, Object>
{
    HttpApplication getApplication();

    Object getSession();

    HttpSession getOrCreateSession();

    boolean isSecure();

    String getClientAddress();

    String getProtocol();

    HttpMethod getMethod();

    URI getUrl();

    Map<String, List<String>> getQueryParameters();

    Map<String, String> getPathParameters();

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
