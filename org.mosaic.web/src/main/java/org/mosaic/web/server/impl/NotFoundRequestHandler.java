package org.mosaic.web.server.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.server.HttpStatus;
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "methods", value = "get,head,put,post,delete"))
@Ranking(Integer.MIN_VALUE)
final class NotFoundRequestHandler implements RequestHandler
{
    @Nonnull
    @Component
    private RequestHandlersManagerImpl requestHandlersManager;

    @Override
    public boolean canHandle( @Nonnull WebInvocation request )
    {
        return true;
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation ) throws Throwable
    {
        // TODO: add application-specific not-found handling, maybe error page, etc
        invocation.getHttpResponse().setStatus( HttpStatus.NOT_FOUND, "Not found" );
        invocation.disableCaching();
        return null;
    }
}
