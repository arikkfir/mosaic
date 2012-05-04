package org.mosaic.server.web;

import org.mosaic.util.logging.LogContext;
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
        if( request != null )
        {
            LogContext.put( "appName", request.getApplication().getName() );
            LogContext.put( "reqClient", request.getClientAddress() );
            LogContext.put( "reqMethod", request.getMethod() );
            LogContext.put( "reqPath", request.getUrl().getPath() );
        }
        else
        {
            LogContext.remove( "appName" );
            LogContext.remove( "reqClient" );
            LogContext.remove( "reqMethod" );
            LogContext.remove( "reqPath" );
        }
    }
}
