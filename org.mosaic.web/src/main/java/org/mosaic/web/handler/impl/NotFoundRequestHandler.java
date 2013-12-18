package org.mosaic.web.handler.impl;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;

/**
 * @author arik
 */
@Service
@Ranking(Integer.MIN_VALUE)
final class NotFoundRequestHandler implements RequestHandler
{
    private static final Set<String> HTTP_METHODS = Sets.newHashSet( "GET", "HEAD", "PUT", "POST", "DELETE" );

    @Nonnull
    @Component
    private RequestHandlersManagerImpl requestHandlersManager;

    @Nonnull
    @Override
    public Set<String> getHttpMethods()
    {
        return HTTP_METHODS;
    }

    @Override
    public boolean canHandle( @Nonnull WebRequest request )
    {
        return true;
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request ) throws Throwable
    {
        // TODO: add application-specific not-found handling, maybe error page, etc
        WebResponse response = request.getResponse();
        response.setStatus( HttpStatus.NOT_FOUND );
        response.disableCaching();
        return null;
    }
}
