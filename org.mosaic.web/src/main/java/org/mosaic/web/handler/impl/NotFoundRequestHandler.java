package org.mosaic.web.handler.impl;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.http.HttpStatus;
import org.mosaic.web.request.WebInvocation;

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
    public boolean canHandle( @Nonnull WebInvocation request )
    {
        return true;
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation request ) throws Throwable
    {
        // TODO: add application-specific not-found handling, maybe error page, etc
        request.getHttpResponse().setStatus( HttpStatus.NOT_FOUND, "Not found" );
        request.disableCaching();
        return null;
    }
}
