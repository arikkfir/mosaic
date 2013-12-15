package org.mosaic.web.handler.impl;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
@Component
public class RequestHandlerManager
{
    @Nonnull
    @Service
    private List<RequestHandler> requestHandlers;

    @Nullable
    public RequestHandler findRequestHandler( @Nonnull WebRequest request )
    {
        // TODO: perform some kind of caching of matching handlers - maybe based on URIs?
        for( RequestHandler requestHandler : this.requestHandlers )
        {
            if( requestHandler.canHandle( request ) )
            {
                return requestHandler;
            }
        }
        return null;
    }
}
