package org.mosaic.web.util;

import org.mosaic.collection.TypedDict;
import org.mosaic.web.HttpApplication;
import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public abstract class Http {

    private static final ThreadLocal<HttpApplication> APPLICATION = new ThreadLocal<>();

    private static final ThreadLocal<HttpRequest> REQUEST = new ThreadLocal<>();

    private static final ThreadLocal<TypedDict<String>> URI_PARAMS = new ThreadLocal<>();

    public static HttpRequest uriParams() {
        return REQUEST.get();
    }

    public static void setUriParams( TypedDict<String> uriParams ) {
        URI_PARAMS.set( uriParams );
    }

    public static HttpRequest request() {
        return REQUEST.get();
    }

    public static HttpRequest requireRequest() {
        HttpRequest request = request();
        if( request == null ) {
            Thread thread = Thread.currentThread();
            throw new IllegalStateException( "HTTP request not bound to current thread (" + thread.getName() + "[" + thread.getId() + "])" );
        } else {
            return request;
        }
    }

    public static void setRequest( HttpRequest request ) {
        REQUEST.set( request );
    }

    public static HttpApplication application() {
        return APPLICATION.get();
    }

    public static HttpApplication requireApplication() {
        HttpApplication application = application();
        if( application == null ) {
            Thread thread = Thread.currentThread();
            throw new IllegalStateException( "HTTP application not bound to current thread (" + thread.getName() + "[" + thread.getId() + "])" );
        } else {
            return application;
        }
    }

    public static void setApplication( HttpApplication application ) {
        APPLICATION.set( application );
    }
}
