package org.mosaic.web.handler.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.UriInterceptor;
import org.mosaic.web.handler.spi.HttpMethodMarker;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter(InterceptorAdapter.class)
final class UriInterceptorAdapter extends InterceptorAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( UriInterceptorAdapter.class );

    private static final String PATH_PARAMS_ATTR_KEY = UriInterceptorAdapter.class.getName() + ".pathParameters";

    @Nonnull
    private final MethodEndpoint<UriInterceptor> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    @Nullable
    private final Expression<Boolean> applicationExpression;

    @Nonnull
    private final Set<String> httpMethods;

    @Nonnull
    private final String uri;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Service
    UriInterceptorAdapter( @Nonnull MethodEndpoint<UriInterceptor> endpoint )
    {
        this.endpoint = endpoint;

        String appExpr = this.endpoint.getType().app();
        if( appExpr.isEmpty() )
        {
            this.applicationExpression = null;
        }
        else
        {
            this.applicationExpression = this.expressionParser.parseExpression( appExpr, Boolean.class );
        }

        Set<String> httpMethods = new HashSet<>();
        for( Annotation annotation : this.endpoint.getMethodHandle().getAnnotations() )
        {
            if( annotation.annotationType().isAnnotationPresent( HttpMethodMarker.class ) )
            {
                httpMethods.add( annotation.annotationType().getSimpleName().toUpperCase() );
            }
        }
        this.httpMethods = httpMethods;

        this.uri = this.endpoint.getType().uri();

        // TODO: add more parameter resolvers
        this.invoker = this.endpoint.createInvoker(
                new SimpleParameterResolver( "request", WebRequest.class ),
                new SimpleParameterResolver( "interceptorChain", InterceptorChain.class )
        );
    }

    @Nonnull
    @Override
    Set<String> getHttpMethods()
    {
        return this.httpMethods;
    }

    boolean canHandle( @Nonnull WebRequest request, @Nonnull RequestHandler requestHandler )
    {
        if( this.applicationExpression != null && !this.applicationExpression.createInvocation( request ).require() )
        {
            return false;
        }

        MapEx<String, String> pathParameters = request.getUri().getPathParameters( this.uri );
        if( pathParameters == null )
        {
            return false;
        }

        request.getAttributes().put( PATH_PARAMS_ATTR_KEY, pathParameters );
        return true;
    }

    @Nullable
    Object handle( @Nonnull WebRequest request, @Nonnull InterceptorChain interceptorChain ) throws Exception
    {
        LOG.debug( "Invoking URI interceptor '{}'", this.endpoint );

        Map<String, Object> context = new HashMap<>();
        context.put( "request", request );
        context.put( "interceptorChain", interceptorChain );
        return this.invoker.resolve( context ).invoke();
    }
}
