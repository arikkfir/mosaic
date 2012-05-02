package org.mosaic.server.web.dispatcher.impl.handler;

import java.util.ArrayList;
import java.util.List;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Handler;
import org.mosaic.web.handler.Interceptor;

/**
 * @author arik
 */
public class RequestExecutionPlan
{

    private final HttpRequest request;

    private Handler handler;

    private Handler.HandlerMatch handlerMatch;

    private List<InterceptorEntry> interceptors = new ArrayList<>( );

    public RequestExecutionPlan( HttpRequest request )
    {
        this.request = request;
    }

    public HttpRequest getRequest( )
    {
        return request;
    }

    public void setHandler( Handler handler, Handler.HandlerMatch match )
    {
        this.handler = handler;
        this.handlerMatch = match;
    }

    public void addInterceptor( Interceptor interceptor, Interceptor.InterceptorMatch match )
    {
        this.interceptors.add( new InterceptorEntry( interceptor, match ) );
    }

    private class InterceptorEntry
    {

        private final Interceptor interceptor;

        private final Interceptor.InterceptorMatch interceptorMatch;

        private InterceptorEntry( Interceptor interceptor, Interceptor.InterceptorMatch interceptorMatch )
        {
            this.interceptor = interceptor;
            this.interceptorMatch = interceptorMatch;
        }
    }
}
