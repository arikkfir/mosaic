package org.mosaic.server.web.dispatcher.impl.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.server.web.PathParamsAware;
import org.mosaic.server.web.dispatcher.impl.handler.parameters.MethodParameterResolver;
import org.mosaic.server.web.dispatcher.impl.util.RegexPathMatcher;
import org.mosaic.web.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.mosaic.server.web.dispatcher.impl.util.HandlerUtils.*;

/**
 * @author arik
 */
public abstract class AbstractMethodEndpointManager
{
    private List<MethodParameterResolver> methodParameterResolvers;

    @Autowired
    public void setMethodParameterResolvers( List<MethodParameterResolver> methodParameterResolvers )
    {
        this.methodParameterResolvers = methodParameterResolvers;
    }

    protected class MethodEndpointWrapper
    {
        private final MethodEndpointInfo methodEndpointInfo;

        private final List<RegexPathMatcher> pathMatchers;

        private final List<MethodParameterResolver.ResolvedParameter> parameters;

        private final Expression filterExpression;

        protected MethodEndpointWrapper( MethodEndpointInfo methodEndpointInfo,
                                         Class<? extends Annotation> pathPatternsAnnotation )
        {
            this.methodEndpointInfo = methodEndpointInfo;
            this.pathMatchers = getMethodPaths( this.methodEndpointInfo.getMethod(), pathPatternsAnnotation );
            this.filterExpression = getMethodFilter( this.methodEndpointInfo.getMethod() );
            this.parameters = resolveMethodParameters( methodParameterResolvers, this.methodEndpointInfo.getMethod() );
        }

        protected RegexPathMatcher.MatchResult accepts( HttpRequest request )
        {
            // check filter
            if( this.filterExpression != null )
            {
                Boolean result = this.filterExpression.getValue( new StandardEvaluationContext( request ), Boolean.class );
                if( result == null || !result )
                {
                    return null;
                }
            }

            // TODO: check HTTP methods

            // check paths
            String path = request.getUrl().getPath();
            for( RegexPathMatcher matcher : this.pathMatchers )
            {
                RegexPathMatcher.MatchResult match = matcher.match( path );
                if( match.isMatching() )
                {
                    return match;
                }
            }
            return null;
        }

        protected Map<String, String> pushPathParams( HttpRequest request, Map<String, String> newPathParams )
        {
            Map<String, String> oldPathParams = request.getPathParameters();
            PathParamsAware pathParamsAware;
            if( request instanceof PathParamsAware )
            {
                pathParamsAware = ( PathParamsAware ) request;
                pathParamsAware.setPathParams( newPathParams );
                return oldPathParams;
            }
            else
            {
                throw new IllegalStateException( "HttpRequest does not implement PathParamsAware! (" + request + ")" );
            }
        }

        protected Object invoke( HttpRequest request ) throws InvocationTargetException, IllegalAccessException
        {
            if( this.parameters.isEmpty() )
            {
                return this.methodEndpointInfo.invoke();
            }
            else
            {
                Object[] arguments = new Object[ this.parameters.size() ];
                for( int i = 0; i < this.parameters.size(); i++ )
                {
                    arguments[ i ] = this.parameters.get( i ).resolve( request );
                }
                return this.methodEndpointInfo.invoke( arguments );
            }
        }

        protected void popPathParams( HttpRequest request, Map<String, String> oldPathParams )
        {
            PathParamsAware pathParamsAware;
            if( request instanceof PathParamsAware )
            {
                pathParamsAware = ( PathParamsAware ) request;
                pathParamsAware.setPathParams( oldPathParams );
            }
            else
            {
                throw new IllegalStateException( "HttpRequest does not implement PathParamsAware! (" + request + ")" );
            }
        }
    }
}
