package org.mosaic.web.impl;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
@Service
@Ranking(-1)
final class OptionsRequestHandler implements RequestHandler
{
    private static final HashSet<String> HTTP_METHODS = Sets.newHashSet( "OPTIONS" );

    @Nonnull
    @Component
    private RequestHandlerManager requestHandlerManager;

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
        List<RequestHandler> handlers = this.requestHandlerManager.findRequestHandlers( request );

        Set<String> httpMethods = Sets.newHashSet( "OPTIONS" );
        for( RequestHandler handler : handlers )
        {
            httpMethods.addAll( handler.getHttpMethods() );
        }
        request.getResponse().getHeaders().setAllow( new LinkedList<>( httpMethods ) );
        return null;
    }
}
