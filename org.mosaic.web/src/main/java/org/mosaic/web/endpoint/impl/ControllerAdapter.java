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
import org.mosaic.web.endpoint.Controller;
import org.mosaic.web.endpoint.HttpMethodMarker;
import org.mosaic.web.endpoint.Secured;
import org.mosaic.web.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.mosaic.util.reflection.TypeTokens.BOOLEAN;

/**
 * @author arik
 */
@Adapter(RequestHandler.class)
final class ControllerAdapter implements RequestHandler, org.mosaic.web.server.Secured, ServicePropertiesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( ControllerAdapter.class );

    static final String PATH_PARAMS_ATTR_KEY = ControllerAdapter.class.getName() + ".pathParameters";

    @Nonnull
    private final MethodEndpoint<Controller> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    @Nullable
    private final Expression<Boolean> applicationExpression;

    @Nonnull
    private final Set<String> httpMethods;

    @Nonnull
    private final String uri;

    @Nullable
    private final ControllerSecurityConstraint securityConstraint;

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Service
    ControllerAdapter( @Nonnull MethodEndpoint<Controller> endpoint )
    {
        this.endpoint = endpoint;

        String appExpr = this.endpoint.getType().app();
        if( appExpr.isEmpty() )
        {
            this.applicationExpression = null;
        }
        else
        {
            this.applicationExpression = this.expressionParser.parseExpression( appExpr, BOOLEAN );
        }

        this.uri = this.endpoint.getType().uri();

        Set<String> httpMethods = new HashSet<>();
        for( Annotation annotation : this.endpoint.getMethodHandle().getAnnotations() )
        {
            if( annotation.annotationType().isAnnotationPresent( HttpMethodMarker.class ) )
            {
                httpMethods.add( annotation.annotationType().getSimpleName().toUpperCase() );
            }
        }
        this.httpMethods = httpMethods;

        Secured securedAnn = this.endpoint.getMethodHandle().getAnnotation( Secured.class );
        this.securityConstraint = securedAnn != null ? new ControllerSecurityConstraint( securedAnn ) : null;

        // TODO: add resolvers for request query, headers, cookies etc
        this.invoker = this.endpoint.createInvoker(
                new SimpleParameterResolver<>( "invocation", WebInvocation.class ),
                new SimpleParameterResolver<>( "request", HttpRequest.class ),
                new SimpleParameterResolver<>( "response", HttpResponse.class )
        );
    }

    @Override
    public void addProperties( @Nonnull Dictionary<String, Object> properties )
    {
        properties.put( "methods", new LinkedList<>( this.httpMethods ) );
    }

    @Nullable
    @Override
    public SecurityConstraint getSecurityConstraint( @Nonnull WebInvocation request )
    {
        return this.securityConstraint;
    }

    @Override
    public boolean canHandle( @Nonnull WebInvocation request )
    {
        if( this.applicationExpression != null )
        {
            Boolean result = this.applicationExpression.createInvocation( request ).invoke();
            if( result == null || !result )
            {
                return false;
            }
        }

        MapEx<String, String> pathParameters = request.getHttpRequest().getUri().getPathParameters( this.uri );
        if( pathParameters == null )
        {
            return false;
        }

        request.getAttributes().put( PATH_PARAMS_ATTR_KEY, pathParameters );
        return true;
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation ) throws Exception
    {
        LOG.debug( "Invoking method endpoint '{}'", this.endpoint );
        Map<String, Object> context = new HashMap<>();
        context.put( "invocation", invocation );
        context.put( "request", invocation.getHttpRequest() );
        context.put( "response", invocation.getHttpResponse() );
        return this.invoker.resolve( context ).invoke();
    }

    private class ControllerSecurityConstraint implements SecurityConstraint
    {
        @Nullable
        private final List<String> authenticationMethods;

        @Nullable
        private final String challangeMethod;

        @Nullable
        private final Expression<Boolean> authExpression;

        public ControllerSecurityConstraint( @Nonnull Secured secured )
        {
            this.authenticationMethods = secured.authMethods().length > 0 ? asList( secured.authMethods() ) : null;
            this.challangeMethod = !secured.challangeMethod().isEmpty() ? secured.challangeMethod() : null;
            this.authExpression = !secured.value().isEmpty() ? expressionParser.parseExpression( secured.value(), BOOLEAN ) : null;
        }

        @Nullable
        @Override
        public Collection<String> getAuthenticationMethods()
        {
            return this.authenticationMethods;
        }

        @Nullable
        @Override
        public Expression<Boolean> getExpression()
        {
            return this.authExpression;
        }

        @Nullable
        @Override
        public String getChallangeMethod()
        {
            return this.challangeMethod;
        }
    }
}
