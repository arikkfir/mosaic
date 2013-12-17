package org.mosaic.web.impl;

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
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.TypedInterceptor;
import org.mosaic.web.handler.spi.HttpMethodMarker;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter(InterceptorAdapter.class)
final class TypedInterceptorAdapter extends InterceptorAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( TypedInterceptorAdapter.class );

    private static final String PATH_PARAMS_ATTR_KEY = TypedInterceptorAdapter.class.getName() + ".pathParameters";

    @Nonnull
    private final MethodEndpoint<TypedInterceptor> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    @Nullable
    private final Expression<Boolean> applicationExpression;

    @Nonnull
    private final Set<String> httpMethods;

    @Nonnull
    private final Class<? extends Annotation> handlerType;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Service
    TypedInterceptorAdapter( @Nonnull MethodEndpoint<TypedInterceptor> endpoint )
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

        this.handlerType = this.endpoint.getType().value().asSubclass( Annotation.class );

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

        MapEx<String, Object> attributes = request.getAttributes();

        if( this.handlerType.equals( Controller.class ) && requestHandler instanceof ControllerAdapter )
        {
            attributes.put( PATH_PARAMS_ATTR_KEY, attributes.require( ControllerAdapter.PATH_PARAMS_ATTR_KEY ) );
            return true;
        }
        else
        {
            return this.handlerType.isInstance( requestHandler );
        }
    }

    @Nullable
    Object handle( @Nonnull WebRequest request, @Nonnull InterceptorChain interceptorChain ) throws Exception
    {
        LOG.debug( "Invoking typed interceptor '{}'", this.endpoint );

        Map<String, Object> context = new HashMap<>();
        context.put( "request", request );
        context.put( "interceptorChain", interceptorChain );
        return this.invoker.resolve( context ).invoke();
    }
}
