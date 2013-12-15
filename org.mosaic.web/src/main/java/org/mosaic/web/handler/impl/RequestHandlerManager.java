package org.mosaic.web.handler.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebRequest;

import static java.util.Collections.singletonList;

/**
 * @author arik
 */
@Component
public final class RequestHandlerManager
{
    @Nonnull
    @Service
    private List<RequestHandler> requestHandlers;

    @Nonnull
    public List<RequestHandler> findRequestHandlers( @Nonnull WebRequest request, int max )
    {
        // TODO: perform some kind of caching of matching handlers - maybe based on URIs?

        String method = request.getMethod();

        List<RequestHandler> handlers = null;
        for( RequestHandler requestHandler : this.requestHandlers )
        {
            if( !method.equals( "OPTIONS" ) )
            {
                Set<String> httpMethods = requestHandler.getHttpMethods();
                if( httpMethods == null && !method.equals( "GET" ) && !method.equals( "HEAD" ) )
                {
                    continue;
                }
                else if( httpMethods != null && !httpMethods.contains( method ) )
                {
                    continue;
                }
            }

            if( requestHandler.canHandle( request ) )
            {
                if( max == 1 )
                {
                    return singletonList( requestHandler );
                }
                else
                {
                    if( handlers == null )
                    {
                        handlers = new LinkedList<>();
                    }
                    handlers.add( requestHandler );

                    // if we have enough handlers, exit now
                    if( max > 0 && handlers.size() >= max )
                    {
                        return handlers;
                    }
                }
            }
        }

        return handlers == null ? Collections.<RequestHandler>emptyList() : handlers;
    }
}
