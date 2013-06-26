package org.mosaic.web.handler.impl.adapter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class RequestHandlerEndpointAdapter extends AbstractPathMethodEndpointAdapter
{
    public RequestHandlerEndpointAdapter( long id,
                                          int rank,
                                          @Nonnull MethodEndpoint endpoint,
                                          @Nonnull ExpressionParser expressionParser,
                                          @Nonnull ConversionService conversionService )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        super( id, rank, endpoint, expressionParser, conversionService );
    }

    @Nullable
    public PathHandlerContext matches( @Nonnull WebRequest request )
    {
        if( !matchesWebApplication( request ) )
        {
            return null;
        }

        for( String pathTemplate : this.pathTemplates )
        {
            MapEx<String, String> pathParameters = request.getUri().getPathParameters( pathTemplate );
            if( pathParameters != null )
            {
                return new PathHandlerContext( request, pathTemplate, pathParameters );
            }
        }

        return null;
    }

    @Nullable
    public Object handle( @Nonnull HandlerContext context ) throws Exception
    {
        WebRequest request = context.getRequest();
        if( !isUserAllowed( request ) )
        {
            if( request.getUser().isAnonymous() )
            {
                request.getResponse().setStatus( HttpStatus.UNAUTHORIZED );
            }
            else
            {
                request.getResponse().setStatus( HttpStatus.FORBIDDEN );
            }
        }

        Map<String, Object> resolveContext = new HashMap<>();
        resolveContext.put( "handlerContext", context );
        return getEndpointInvoker().resolve( resolveContext ).invoke();
    }
}
