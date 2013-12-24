package org.mosaic.web.handler.impl;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.security.AccessDeniedException;
import org.mosaic.security.Security;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.application.Application;
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.spi.HttpMethodMarker;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.security.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter( RequestHandler.class )
final class ControllerAdapter implements RequestHandler
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

    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Nonnull
    @Service
    private List<Authenticator> authenticators;

    @Nonnull
    @Service
    private Security security;

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
            this.applicationExpression = this.expressionParser.parseExpression( appExpr, Boolean.class );
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

        // TODO: add more parameter resolvers
        this.invoker = this.endpoint.createInvoker(
                new SimpleParameterResolver( "request", WebRequest.class )
        );
    }

    @Nonnull
    @Override
    public Set<String> getHttpMethods()
    {
        return this.httpMethods;
    }

    @Override
    public boolean canHandle( @Nonnull WebRequest request )
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
    @Override
    public Object handle( @Nonnull WebRequest request ) throws Exception
    {
        LOG.debug( "Invoking method endpoint '{}'", this.endpoint );
        try
        {
            return this.invoker.resolve( Collections.<String, Object>singletonMap( "request", request ) ).invoke();
        }
        catch( AccessDeniedException e )
        {
            // if user already authenticated, no use in re-authenticating - just deny access as well
            if( this.security.getSubject().isAuthenticated() )
            {
                request.getResponse().setStatus( HttpStatus.FORBIDDEN );
                return null;
            }

            // get auth method from exception; if none, see if request matches a security constraint
            String authMethod = e.getAuthenticationMethod();
            if( authMethod == null )
            {
                Application.ApplicationSecurity.SecurityConstraint securityConstraint = request.getSecurityConstraint();
                if( securityConstraint != null )
                {
                    authMethod = securityConstraint.getAuthenticationMethod();
                }
            }

            // if not found auth method (exception / security-constraint), just deny access
            if( authMethod == null )
            {
                // if no security constraint, we don't know HOW users should authenticate, so just deny access
                request.getResponse().setStatus( HttpStatus.FORBIDDEN );
                return null;
            }

            // we have auth method - find appropriate authenticator and use it to send a challange to the client
            for( Authenticator authenticator : this.authenticators )
            {
                if( authenticator.getAuthenticationMethods().contains( authMethod ) )
                {
                    authenticator.challange( request );
                    return null;
                }
            }

            // if we got here - no authenticator matched; fail request and log
            request.getResponse().setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
            request.dumpToWarnLog( LOG, "Could not find '{}' authenticator", authMethod, e );
            return null;
        }
    }
}
