package org.mosaic.server.web;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public abstract class Http
{
    private static final ThreadLocal<HttpRequest> REQUEST = new ThreadLocal<>();

    public static HttpRequest request()
    {
        return REQUEST.get();
    }

    public static void setRequest( HttpRequest request )
    {
        REQUEST.set( request );
    }
}
