package org.mosaic.web.server.impl;

import com.google.common.collect.Sets;
import java.util.LinkedList;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "methods", value = "options"))
@Ranking(-1)
final class OptionsRequestHandler implements RequestHandler
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
        Set<String> httpMethods = Sets.newHashSet( "OPTIONS" );
        httpMethods.addAll( this.requestHandlersManager.findRequestHandlersByMethod( invocation ).keySet() );
        invocation.getHttpResponse().setAllow( new LinkedList<>( httpMethods ) );
        return null;
    }
}
