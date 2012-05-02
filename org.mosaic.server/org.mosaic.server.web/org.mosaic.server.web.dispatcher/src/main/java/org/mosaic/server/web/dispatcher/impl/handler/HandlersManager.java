package org.mosaic.server.web.dispatcher.impl.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.security.AccessDeniedException;
import org.mosaic.server.web.PathParamsAware;
import org.mosaic.server.web.dispatcher.impl.handler.parameters.MethodParameterInfo;
import org.mosaic.server.web.dispatcher.impl.handler.parameters.MethodParameterResolver;
import org.mosaic.server.web.dispatcher.impl.util.HandlerUtils;
import org.mosaic.server.web.dispatcher.impl.util.RegexPathMatcher;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Handler;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.Filter;
import org.mosaic.web.handler.annotation.Secured;
import org.mosaic.web.handler.annotation.Service;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class HandlersManager
{
    protected final Logger LOG = LoggerFactory.getLogger( getClass( ) );

    private final HandlersManager.NotFoundHandler notFoundHandler = new NotFoundHandler( );

    private ConversionService conversionService;

    private List<MethodParameterResolver> methodParameterResolvers;

    private List<HandlerEntry> handlers = Collections.emptyList( );

    @Autowired
    public void setConversionService( ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Autowired
    public void setMethodParameterResolvers( List<MethodParameterResolver> methodParameterResolvers )
    {
        this.methodParameterResolvers = methodParameterResolvers;
    }

    @ServiceBind
    public synchronized void addNativeHandler( ServiceReference<?> ref, Handler handler )
    {
        List<HandlerEntry> newHandlers = new ArrayList<>( this.handlers );
        newHandlers.add( new HandlerEntry( ref, handler ) );
        Collections.sort( newHandlers );
        this.handlers = newHandlers;
    }

    @ServiceUnbind
    public synchronized void removeNativeHandler( ServiceReference<?> ref )
    {
        List<HandlerEntry> newHandlers = new ArrayList<>( this.handlers );
        for( Iterator<HandlerEntry> iterator = newHandlers.iterator( ); iterator.hasNext( ); )
        {
            HandlerEntry entry = iterator.next( );
            if( entry.ref.equals( ref ) )
            {
                iterator.remove( );
                this.handlers = newHandlers;
                return;
            }
        }
    }

    @ServiceBind( filter = "methodEndpointShortType=Controller" )
    public void addController( ServiceReference<?> ref, MethodEndpointInfo endpointInfo )
    {
        addNativeHandler( ref, new MethodEndpointHandler( endpointInfo, Controller.class ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Controller" )
    public void removeController( ServiceReference<?> ref )
    {
        removeNativeHandler( ref );
    }

    @ServiceBind( filter = "methodEndpointShortType=Service" )
    public void addService( ServiceReference<?> ref, MethodEndpointInfo endpointInfo )
    {
        addNativeHandler( ref, new MethodEndpointHandler( endpointInfo, Service.class ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Service" )
    public void removeService( ServiceReference<?> ref )
    {
        removeNativeHandler( ref );
    }

    public void applyHandler( RequestExecutionPlan plan )
    {
        for( int i = this.handlers.size( ) - 1; i >= 0; i-- )
        {
            HandlerEntry entry = this.handlers.get( i );

            Handler.HandlerMatch match = entry.handler.matches( plan.getRequest( ) );
            if( match != null )
            {
                plan.setHandler( entry.handler, match );
                return;
            }
        }
        plan.setHandler( this.notFoundHandler, this.notFoundHandler );
    }

    private class HandlerEntry implements Comparable<HandlerEntry>
    {
        private final ServiceReference<?> ref;

        private final Handler handler;

        private HandlerEntry( ServiceReference<?> ref, Handler handler )
        {
            this.ref = ref;
            this.handler = handler;
        }

        public Integer getRank( )
        {
            return ( Integer ) this.ref.getProperty( Constants.SERVICE_RANKING );
        }

        @Override
        public int compareTo( HandlerEntry o )
        {
            Integer myRank = getRank( );
            Integer thatRank = o.getRank( );
            if( myRank.equals( thatRank ) )
            {
                return 0;
            }
            else if( myRank < thatRank )
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }

    private class NotFoundHandler implements Handler, Handler.HandlerMatch
    {
        @Override
        public HandlerMatch matches( HttpRequest request )
        {
            return this;
        }

        @Override
        public Object handle( HttpRequest request, HandlerMatch match ) throws Exception
        {
            request.getResponseHeaders( ).disableCache( );
            request.setResponseStatus( HttpStatus.NOT_FOUND, "Unknown URI" );
            return null;
        }
    }

    private class MethodEndpointHandlerMatch implements Handler.HandlerMatch
    {
        private final RegexPathMatcher.MatchResult matchResult;

        private final TypedDict<String> pathParams;

        private MethodEndpointHandlerMatch( RegexPathMatcher.MatchResult matchResult )
        {
            this.matchResult = matchResult;
            this.pathParams = new WrappingTypedDict<>( conversionService, String.class );
            for( Map.Entry<String, String> entry : this.matchResult.getVariables( ).entrySet( ) )
            {
                this.pathParams.add( entry.getKey( ), entry.getValue( ) );
            }
        }

        private TypedDict<String> getPathParams( )
        {
            return this.pathParams;
        }
    }

    private class MethodEndpointHandler implements Handler
    {
        private final MethodEndpointInfo methodEndpointInfo;

        private final List<RegexPathMatcher> pathMatchers = new LinkedList<>( );

        private final List<MethodParameterResolver.ResolvedParameter> parameters;

        private Expression filterExpression;

        private Expression securityExpression;

        private MethodEndpointHandler( MethodEndpointInfo methodEndpointInfo,
                                       Class<? extends Annotation> pathPatternsAnnotation )
        {
            this.methodEndpointInfo = methodEndpointInfo;

            Method method = this.methodEndpointInfo.getMethod( );

            // path patterns
            Annotation pathPatternsAnn = HandlerUtils.findAnn( method, pathPatternsAnnotation );
            if( pathPatternsAnn == null )
            {
                throw new IllegalArgumentException( "Method endpoint has no @" +
                                                    pathPatternsAnnotation.getSimpleName( ) +
                                                    " annotation: " +
                                                    this.methodEndpointInfo );
            }
            else
            {
                String[] pathPatterns = ( String[] ) AnnotationUtils.getValue( pathPatternsAnn );
                for( String pathPattern : pathPatterns )
                {
                    this.pathMatchers.add( new RegexPathMatcher( pathPattern ) );
                }
            }

            // filter
            Filter filterAnn = HandlerUtils.findAnn( method, Filter.class );
            if( filterAnn != null )
            {
                this.filterExpression = new SpelExpressionParser( ).parseExpression( filterAnn.value( ) );
            }

            // security
            Secured securedAnn = HandlerUtils.findAnn( method, Secured.class );
            if( securedAnn != null )
            {
                if( securedAnn.value( ).trim( ).length( ) == 0 )
                {
                    this.securityExpression = new SpelExpressionParser( ).parseExpression( "user != null" );
                }
                else
                {
                    this.securityExpression = new SpelExpressionParser( ).parseExpression( securedAnn.value( ) );
                }
            }


            List<MethodParameterResolver.ResolvedParameter> resolvedParameters = new LinkedList<>( );
            for( int i = 0; i < method.getParameterTypes( ).length; i++ )
            {
                MethodParameterInfo parameter = new MethodParameterInfo( method, i );
                for( MethodParameterResolver resolver : methodParameterResolvers )
                {
                    MethodParameterResolver.ResolvedParameter resolvedParameter = resolver.resolve( parameter );
                    if( resolvedParameter != null )
                    {
                        resolvedParameters.add( resolvedParameter );
                        break;
                    }
                }
            }
            this.parameters = resolvedParameters;
        }

        @Override
        public HandlerMatch matches( HttpRequest request )
        {
            if( this.filterExpression != null )
            {
                Boolean result =
                        this.filterExpression.getValue( new StandardEvaluationContext( request ), Boolean.class );
                if( result == null || !result )
                {
                    // request filter not passed - dont handle this request
                    LOG.debug( "Handler {} ignores request", this.methodEndpointInfo );
                    return null;
                }
            }

            String path = request.getUrl( ).getPath( );
            for( RegexPathMatcher matcher : this.pathMatchers )
            {
                RegexPathMatcher.MatchResult match = matcher.match( path );
                if( match.isMatching( ) )
                {
                    return new MethodEndpointHandlerMatch( match );
                }
            }

            return null;
        }

        @Override
        public Object handle( HttpRequest request, HandlerMatch match ) throws Exception
        {
            MethodEndpointHandlerMatch methodEndpointHandlerMatch = ( MethodEndpointHandlerMatch ) match;

            // set path params on request
            TypedDict<String> oldPathParams = request.getPathParameters( );
            PathParamsAware pathParamsAware;
            if( request instanceof PathParamsAware )
            {
                pathParamsAware = ( PathParamsAware ) request;
                pathParamsAware.setPathParams( methodEndpointHandlerMatch.getPathParams( ) );
            }
            else
            {
                throw new IllegalStateException( "HttpRequest does not implement PathParamsAware! (" + request + ")" );
            }

            // try-finally block since we must revert path params to previous value no matter what (think interceptors...)
            try
            {
                // check security
                if( this.securityExpression != null )
                {
                    Boolean result =
                            this.securityExpression.getValue( new StandardEvaluationContext( request ), Boolean.class );
                    if( result == null || !result )
                    {
                        // security filter not passed - dont handle this request
                        LOG.debug( "Handler {} ignores request", this.methodEndpointInfo );
                        throw new AccessDeniedException( );
                    }
                }

                // invoke the method
                if( this.parameters.isEmpty( ) )
                {
                    return this.methodEndpointInfo.invoke( );
                }
                else
                {
                    Object[] arguments = new Object[ this.parameters.size( ) ];
                    for( int i = 0; i < this.parameters.size( ); i++ )
                    {
                        arguments[ i ] = this.parameters.get( i ).resolve( request );
                    }
                    return this.methodEndpointInfo.invoke( arguments );
                }
            }
            finally
            {
                pathParamsAware.setPathParams( oldPathParams );
            }
        }
    }

}
