package org.mosaic.web.server.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.WebInvocation;

import static org.mosaic.web.application.Application.ApplicationResource;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "methods", value = "get,head"))
@Ranking(-1)
final class StaticResourcesRequestHandler implements RequestHandler
{
    @Override
    public boolean canHandle( @Nonnull WebInvocation request )
    {
        String path = request.getHttpRequest().getUri().getDecodedPath();

        ApplicationResource resource = request.getApplication().getResource( path + ".ftl" );
        if( resource == null )
        {
            resource = request.getApplication().getResource( path );
        }

        if( resource != null )
        {
            request.getAttributes().put( "resource", resource );
            return true;
        }
        else
        {
            return false;
        }
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation ) throws Throwable
    {
        return invocation.getAttributes().find( "resource", ApplicationResource.class ).get();
    }
}
