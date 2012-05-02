package org.mosaic.server.web.dispatcher.impl.handler;

import java.lang.annotation.Annotation;
import java.util.*;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.security.AccessDeniedException;
import org.mosaic.server.web.dispatcher.impl.RequestExecutionPlan;
import org.mosaic.server.web.dispatcher.impl.util.RegexPathMatcher;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Handler;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.Service;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

import static java.util.Objects.compare;
import static org.mosaic.server.web.dispatcher.impl.util.HandlerUtils.getMethodSecurity;
import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * @author arik
 */
@Component
public class HandlersManager extends AbstractRequestExecutionBuilder
        implements RequestExecutionPlan.RequestExecutionBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger( HandlersManager.class );

    private NotFoundHandler notFoundHandler;

    private List<HandlerEntry> handlers = Collections.emptyList( );

    @Autowired
    public void setNotFoundHandler( NotFoundHandler notFoundHandler )
    {
        this.notFoundHandler = notFoundHandler;
    }

    @ServiceBind
    public synchronized void addNativeHandler( ServiceReference<Handler> ref, Handler handler )
    {
        addHandler( ref, handler );
    }

    @ServiceUnbind
    public synchronized void removeNativeHandler( ServiceReference<Handler> ref )
    {
        removeHandler( ref );
    }

    @ServiceBind( filter = "methodEndpointShortType=Controller" )
    public void addController( ServiceReference<MethodEndpointInfo> ref, MethodEndpointInfo endpointInfo )
    {
        addHandler( ref, new MethodEndpointHandler( endpointInfo, Controller.class ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Controller" )
    public void removeController( ServiceReference<MethodEndpointInfo> ref )
    {
        removeHandler( ref );
    }

    @ServiceBind( filter = "methodEndpointShortType=Service" )
    public void addService( ServiceReference<MethodEndpointInfo> ref, MethodEndpointInfo endpointInfo )
    {
        addHandler( ref, new MethodEndpointHandler( endpointInfo, Service.class ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Service" )
    public void removeService( ServiceReference<MethodEndpointInfo> ref )
    {
        removeHandler( ref );
    }

    @Override
    public void contribute( RequestExecutionPlan plan )
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

    private void addHandler( ServiceReference<?> ref, Handler handler )
    {
        try
        {
            List<HandlerEntry> newHandlers = new ArrayList<>( this.handlers );
            newHandlers.add( new HandlerEntry( ref, handler ) );
            Collections.sort( newHandlers );
            this.handlers = newHandlers;
        }
        catch( Exception e )
        {
            LOG.warn( "Handler '{}' could not be added to Mosaic handlers: {}", handler, e.getMessage( ), e );
        }
    }

    private void removeHandler( ServiceReference<?> ref )
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

    private class HandlerEntry implements Comparable<HandlerEntry>
    {
        private final ServiceReference<?> ref;

        private final Handler handler;

        private HandlerEntry( ServiceReference<?> ref, Handler handler )
        {
            this.ref = ref;
            this.handler = handler;
        }

        @Override
        public int compareTo( HandlerEntry o )
        {
            Integer myRank = ( Integer ) this.ref.getProperty( SERVICE_RANKING );
            Integer thatRank = ( Integer ) o.ref.getProperty( SERVICE_RANKING );
            return compare( myRank, thatRank, new NullSafeComparator<>( new ComparableComparator<Integer>( ), true ) );
        }
    }

    private class MethodEndpointHandlerMatch implements Handler.HandlerMatch
    {
        private final RegexPathMatcher.MatchResult matchResult;

        private final TypedDict<String> pathParams;

        private MethodEndpointHandlerMatch( RegexPathMatcher.MatchResult matchResult )
        {
            this.matchResult = matchResult;
            this.pathParams = new WrappingTypedDict<>( getConversionService( ), String.class );
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

    private class MethodEndpointHandler extends MethodEndpointWrapper implements Handler
    {
        private final Expression securityExpression;

        private MethodEndpointHandler( MethodEndpointInfo methodEndpointInfo,
                                       Class<? extends Annotation> pathPatternsAnnotation )
        {
            super( methodEndpointInfo, pathPatternsAnnotation );
            this.securityExpression = getMethodSecurity( methodEndpointInfo.getMethod( ) );
        }

        @Override
        public HandlerMatch matches( HttpRequest request )
        {
            RegexPathMatcher.MatchResult match = accepts( request );
            if( match != null )
            {
                return new MethodEndpointHandlerMatch( match );
            }
            else
            {
                return null;
            }
        }

        @Override
        public Object handle( HttpRequest request, HandlerMatch match ) throws Exception
        {
            MethodEndpointHandlerMatch endpointMatch = ( MethodEndpointHandlerMatch ) match;
            TypedDict<String> oldPathParams = pushPathParams( request, endpointMatch.getPathParams( ) );
            try
            {
                // check security
                if( this.securityExpression != null )
                {
                    Boolean result = this.securityExpression.getValue( new StandardEvaluationContext( request ), Boolean.class );
                    if( result == null || !result )
                    {
                        // security filter not passed - don't handle this request
                        throw new AccessDeniedException( );
                    }
                }
                return invoke( request );
            }
            finally
            {
                popPathParams( request, oldPathParams );
            }
        }
    }

}
