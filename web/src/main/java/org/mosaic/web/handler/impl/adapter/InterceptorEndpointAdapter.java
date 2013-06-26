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
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class InterceptorEndpointAdapter extends AbstractPathMethodEndpointAdapter
{
    public InterceptorEndpointAdapter( long id,
                                       int rank,
                                       @Nonnull MethodEndpoint endpoint,
                                       @Nonnull ExpressionParser expressionParser,
                                       @Nonnull ConversionService conversionService )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        super( id, rank, endpoint, expressionParser, conversionService );

        addParameterResolvers( new MethodHandle.ParameterResolver()
        {
            @Nullable
            @Override
            public Object resolve( @Nonnull MethodParameter parameter,
                                   @Nonnull MapEx<String, Object> resolveContext )
            {
                if( parameter.getType().isAssignableFrom( InterceptorChain.class ) )
                {
                    return resolveContext.require( "interceptorChain" );
                }
                return SKIP;
            }
        } );
    }

    @Nullable
    public PathHandlerContext matches( @Nonnull WebRequest request )
    {
        if( !matchesWebApplication( request ) )
        {
            return null;
        }

        if( !matchesHttpMethod( request ) )
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
    public Object handle( @Nonnull HandlerContext context, @Nonnull InterceptorChain interceptorChain ) throws Exception
    {
        Map<String, Object> resolveContext = new HashMap<>();
        resolveContext.put( "handlerContext", context );
        resolveContext.put( "interceptorChain", interceptorChain );
        return getEndpointInvoker().resolve( resolveContext ).invoke();
    }
}
