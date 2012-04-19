package org.mosaic.web;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.mosaic.collection.Dict;
import org.mosaic.collection.TypedDict;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author arik
 * @todo URI parameters should be supported as thread-bound thingy, updated before/after each handler
 * @todo add file-upload support
 */
public interface HttpRequest extends TypedDict<Object> {

    HttpSession getSession();

    HttpSession requireSession();

    boolean isSecure();

    String getClientAddress();

    HttpMethod getMethod();

    URL getUrl();

    TypedDict<String> getQueryParameters();

    HttpRequestHeaders getRequestHeaders();

    InputStream getRequestInputStream();

    Reader getRequestReader();

    Dict<MultipartFile> getFileUploads();

    HttpResponseHeaders getResponseHeaders();

    OutputStream getResponseOutputStream();

    Writer getResponseWriter();

    boolean isCommitted();

}
