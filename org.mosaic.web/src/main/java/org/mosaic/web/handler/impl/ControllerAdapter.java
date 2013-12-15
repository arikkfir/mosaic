package org.mosaic.web.handler.impl;

import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.spi.HttpMethodMarker;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter(RequestHandler.class)
public class ControllerAdapter implements RequestHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( ControllerAdapter.class );

    private static final String PATH_PARAMS_ATTR_KEY = ControllerAdapter.class.getName() + ".pathParameters";

    @Nonnull
    private final MethodEndpoint<Controller> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    @Nonnull
    private final Expression<Boolean> applicationExpression;

    @Nonnull
    private final Set<String> httpMethods;

    @Nonnull
    private final String uri;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Service
    public ControllerAdapter( @Nonnull MethodEndpoint<Controller> endpoint )
    {
        this.endpoint = endpoint;
        this.applicationExpression = this.expressionParser.parseExpression( this.endpoint.getType().app(), Boolean.class );
        this.uri = this.endpoint.getType().uri();

        Set<String> httpMethods = new HashSet<>();
        for( Annotation annotation : this.endpoint.getMethodHandle().getAnnotations() )
        {
            if( annotation.annotationType().isAnnotationPresent( HttpMethodMarker.class ) )
            {
                httpMethods.add( annotation.annotationType().getSimpleName().toUpperCase() );
            }
        }
        this.httpMethods = httpMethods.isEmpty() ? Sets.newHashSet( "GET" ) : httpMethods;

        // TODO: add more parameter resolvers
        this.invoker = this.endpoint.createInvoker(
                new WebRequestParameterResolver()
        );
    }

    @Override
    public boolean canHandle( @Nonnull WebRequest request )
    {
        if( !this.httpMethods.contains( request.getMethod() ) )
        {
            return false;
        }

        if( !this.applicationExpression.createInvocation( request ).require() )
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

    @Override
    public void handle( @Nonnull WebRequest request ) throws Exception
    {
        LOG.debug( "Invoking method endpoint '{}'", this.endpoint );
        this.invoker.resolve( Collections.<String, Object>singletonMap( "request", request ) ).invoke();

        // TODO: apply common headers/status if not set
        // TODO: error handling
    }

    private class WebRequestParameterResolver implements ParameterResolver
    {
        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
                throws Exception
        {
            if( parameter.getType().isAssignableFrom( WebRequest.class ) )
            {
                return resolveContext.require( "request", WebRequest.class );
            }
            return SKIP;
        }
    }
}
