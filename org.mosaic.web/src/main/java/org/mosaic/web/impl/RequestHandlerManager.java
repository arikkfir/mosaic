package org.mosaic.web.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
@Component
final class RequestHandlerManager
{
    @Nonnull
    @Service
    private List<RequestHandler> requestHandlers;

    @Nonnull
    @Service
    private List<InterceptorAdapter> interceptors;

    @Nonnull
    List<RequestHandler> findRequestHandlers( @Nonnull WebRequest request )
    {
        List<RequestHandler> handlers = null;
        for( RequestHandler requestHandler : this.requestHandlers )
        {
            if( requestHandler.canHandle( request ) )
            {
                if( handlers == null )
                {
                    handlers = new LinkedList<>();
                }
                handlers.add( requestHandler );
            }
        }
        return handlers == null ? Collections.<RequestHandler>emptyList() : handlers;
    }

    @Nonnull
    List<InterceptorAdapter> findInterceptors( @Nonnull WebRequest request, @Nonnull RequestHandler requestHandler )
    {
        List<InterceptorAdapter> interceptors = null;
        for( InterceptorAdapter interceptor : this.interceptors )
        {
            if( interceptor.canHandle( request, requestHandler ) )
            {
                if( interceptors == null )
                {
                    interceptors = new LinkedList<>();
                }
                interceptors.add( interceptor );
            }
        }
        return interceptors == null ? Collections.<InterceptorAdapter>emptyList() : interceptors;
    }
}
