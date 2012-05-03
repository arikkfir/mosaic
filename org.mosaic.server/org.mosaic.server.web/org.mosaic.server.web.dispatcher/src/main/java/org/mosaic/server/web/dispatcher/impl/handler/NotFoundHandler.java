package org.mosaic.server.web.dispatcher.impl.handler;

import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Handler;
import org.mosaic.web.handler.UrlNotAllowedException;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class NotFoundHandler implements Handler, Handler.HandlerMatch
{
    @Override
    public HandlerMatch matches( HttpRequest request )
    {
        return this;
    }

    @Override
    public Object handle( HttpRequest request, HandlerMatch match ) throws Exception
    {
        throw new UrlNotAllowedException( request );
    }
}
