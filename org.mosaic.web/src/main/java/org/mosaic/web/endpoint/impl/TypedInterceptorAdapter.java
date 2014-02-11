package org.mosaic.web.endpoint.impl;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServicePropertiesProvider;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.TypeTokens;
import org.mosaic.web.endpoint.Controller;
import org.mosaic.web.endpoint.HttpMethodMarker;
import org.mosaic.web.endpoint.TypedInterceptor;
import org.mosaic.web.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter(RequestInterceptor.class)
final class TypedInterceptorAdapter implements RequestInterceptor, ServicePropertiesProvider
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
    private final Class<?> handlerType;

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
            this.applicationExpression = this.expressionParser.parseExpression( appExpr, TypeTokens.BOOLEAN );
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

        this.handlerType = this.endpoint.getType().value();

        // TODO: add resolvers for request query, headers, cookies etc
        this.invoker = this.endpoint.createInvoker(
                new SimpleParameterResolver<>( "invocation", WebInvocation.class ),
                new SimpleParameterResolver<>( "request", HttpRequest.class ),
                new SimpleParameterResolver<>( "response", HttpResponse.class ),
                new SimpleParameterResolver<>( "chain", InterceptorChain.class ),
                new SimpleParameterResolver<>( "interceptorChain", InterceptorChain.class )
        );
    }

    @Override
    public void addProperties( @Nonnull Dictionary<String, Object> properties )
    {
        properties.put( "methods", new LinkedList<>( this.httpMethods ) );
    }

    @Override
    public boolean canHandle( @Nonnull WebInvocation invocation, @Nonnull RequestHandler requestHandler )
    {
        if( this.applicationExpression != null )
        {
            Boolean result = this.applicationExpression.createInvocation( invocation ).invoke();
            if( result == null || !result )
            {
                return false;
            }
        }

        MapEx<String, Object> attributes = invocation.getAttributes();

        if( this.handlerType.equals( Controller.class ) && requestHandler instanceof ControllerAdapter )
        {
            attributes.put( PATH_PARAMS_ATTR_KEY, attributes.find( ControllerAdapter.PATH_PARAMS_ATTR_KEY ).get() );
            return true;
        }
        else
        {
            return this.handlerType.isInstance( requestHandler );
        }
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation, @Nonnull InterceptorChain interceptorChain )
            throws Exception
    {
        LOG.debug( "Invoking typed interceptor '{}'", this.endpoint );

        Map<String, Object> context = new HashMap<>();
        context.put( "invocation", invocation );
        context.put( "request", invocation.getHttpRequest() );
        context.put( "response", invocation.getHttpResponse() );
        context.put( "chain", interceptorChain );
        context.put( "interceptorChain", interceptorChain );
        return this.invoker.resolve( context ).invoke();
    }
}
