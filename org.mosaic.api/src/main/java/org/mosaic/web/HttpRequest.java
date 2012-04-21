package org.mosaic.web;

import java.io.*;
import java.net.URI;
import java.util.Collection;
import org.mosaic.collection.TypedDict;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * @author arik
 * @todo URI parameters should be supported as thread-bound thingy, updated before/after each handler
 */
public interface HttpRequest extends TypedDict<Object> {

    Object getSession();

    HttpSession getOrCreateSession();

    boolean isSecure();

    String getClientAddress();

    String getProtocol();

    HttpMethod getMethod();

    URI getUrl();

    TypedDict<String> getQueryParameters();

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
