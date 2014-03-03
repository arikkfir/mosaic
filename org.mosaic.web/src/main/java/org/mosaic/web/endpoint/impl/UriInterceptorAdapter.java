package org.mosaic.web.endpoint.impl;

import com.google.common.base.Joiner;
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
import org.mosaic.web.endpoint.HttpMethodMarker;
import org.mosaic.web.endpoint.UriInterceptor;
import org.mosaic.web.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter(RequestInterceptor.class)
final class UriInterceptorAdapter implements RequestInterceptor, ServicePropertiesProvider
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
            this.applicationExpression = this.expressionParser.parseExpression( appExpr, TypeTokens.BOOLEAN );
        }

        Set<String> httpMethods = new HashSet<>();
        for( Annotation annotation : this.endpoint.getMethodHandle().getAnnotations() )
        {
            if( annotation.annotationType().isAnnotationPresent( HttpMethodMarker.class ) )
            {
                httpMethods.add( annotation.annotationType().getSimpleName().toLowerCase() );
            }
        }
        this.httpMethods = httpMethods;

        this.uri = this.endpoint.getType().uri();

        // TODO: add more parameter resolvers
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
        properties.put( "methods", Joiner.on( "," ).join( this.httpMethods ) );
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

        MapEx<String, String> pathParameters = invocation.getHttpRequest().getUri().getPathParameters( this.uri );
        if( pathParameters == null )
        {
            return false;
        }

        invocation.getAttributes().put( PATH_PARAMS_ATTR_KEY, pathParameters );
        return true;
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation, @Nonnull InterceptorChain interceptorChain )
            throws Exception
    {
        LOG.debug( "Invoking URI interceptor '{}'", this.endpoint );

        Map<String, Object> context = new HashMap<>();
        context.put( "invocation", invocation );
        context.put( "request", invocation.getHttpRequest() );
        context.put( "response", invocation.getHttpResponse() );
        context.put( "chain", interceptorChain );
        context.put( "interceptorChain", interceptorChain );
        return this.invoker.resolve( context ).invoke();
    }
}
