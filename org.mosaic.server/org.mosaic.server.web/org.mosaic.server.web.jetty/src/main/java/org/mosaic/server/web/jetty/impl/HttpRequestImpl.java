package org.mosaic.server.web.jetty.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mosaic.collection.Dict;
import org.mosaic.collection.TypedDict;
import org.mosaic.collection.WrappingTypedDict;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.HttpRequestHeaders;
import org.mosaic.web.HttpResponseHeaders;
import org.mosaic.web.HttpSession;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author arik
 */
public class HttpRequestImpl extends WrappingTypedDict<Object> implements HttpRequest {

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final HttpRequestHeadersImpl requestHeaders;

    private final HttpResponseHeadersImpl responseHeaders;

    public HttpRequestImpl( ConversionService conversionService,
                            HttpServletRequest request,
                            HttpServletResponse response ) {
        super( new HashMap<String, List<Object>>(), conversionService, Object.class );
        this.request = request;
        this.response = response;
        this.requestHeaders = new HttpRequestHeadersImpl( this.request, conversionService );
        this.responseHeaders = new HttpResponseHeadersImpl( this.response, conversionService );
    }

    @Override
    public HttpSession getSession() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HttpSession requireSession() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSecure() {
        return this.request.isSecure();
    }

    @Override
    public String getClientAddress() {
        return this.request.getRemoteAddr();
    }

    @Override
    public HttpMethod getMethod() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URL getUrl() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypedDict<String> getQueryParameters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HttpRequestHeaders getRequestHeaders() {
        return this.requestHeaders;
    }

    @Override
    public InputStream getRequestInputStream() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Reader getRequestReader() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Dict<MultipartFile> getFileUploads() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HttpResponseHeaders getResponseHeaders() {
        return this.responseHeaders;
    }

    @Override
    public OutputStream getResponseOutputStream() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Writer getResponseWriter() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isCommitted() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
