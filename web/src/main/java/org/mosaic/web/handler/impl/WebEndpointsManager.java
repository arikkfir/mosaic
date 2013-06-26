package org.mosaic.web.handler.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.ExceptionHandler;
import org.mosaic.web.handler.annotation.Interceptor;
import org.mosaic.web.handler.annotation.WebService;
import org.mosaic.web.handler.impl.adapter.*;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.sort;

/**
 * @author arik
 */
@Bean
public class WebEndpointsManager
{
    private static final Logger LOG = LoggerFactory.getLogger( WebEndpointsManager.class );

    @Nonnull
    private final Collection<ExceptionHandlerEndpointAdapter> exceptionHandlers = new ConcurrentSkipListSet<>();

    @Nonnull
    private final Collection<RequestHandlerEndpointAdapter> handlers = new ConcurrentSkipListSet<>();

    @Nonnull
    private final Collection<InterceptorEndpointAdapter> interceptors = new ConcurrentSkipListSet<>();

    @Nonnull
    private ExpressionParser expressionParser;

    @Nonnull
    private ConversionService conversionService;

    @ServiceRef
    public void setExpressionParser( @Nonnull ExpressionParser expressionParser )
    {
        this.expressionParser = expressionParser;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @MethodEndpointBind(ExceptionHandler.class)
    public void addExceptionHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.exceptionHandlers.add( new ExceptionHandlerEndpointAdapter( id, rank, endpoint, this.expressionParser, this.conversionService ) );
        LOG.info( "Added @ExceptionHandler {}", endpoint );
    }

    @MethodEndpointUnbind(ExceptionHandler.class)
    public void removeExceptionHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.exceptionHandlers );
    }

    @MethodEndpointBind(Controller.class)
    public void addController( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.handlers.add( new ControllerEndpointAdapter( id, rank, endpoint, this.expressionParser, this.conversionService ) );
        LOG.info( "Added @Controller {}", endpoint );
    }

    @MethodEndpointUnbind(Controller.class)
    public void removeController( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.handlers );
    }

    @MethodEndpointBind(WebService.class)
    public void addWebService( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.handlers.add( new WebServiceEndpointAdapter( id, rank, endpoint, this.expressionParser, this.conversionService ) );
        LOG.info( "Added @WebService {}", endpoint );
    }

    @MethodEndpointUnbind(WebService.class)
    public void removeWebService( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.handlers );
    }

    @MethodEndpointBind(Interceptor.class)
    public void addInterceptorHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.interceptors.add( new InterceptorEndpointAdapter( id, rank, endpoint, this.expressionParser, this.conversionService ) );
        LOG.info( "Added @Interceptor {}", endpoint );
    }

    @MethodEndpointUnbind(Interceptor.class)
    public void removeInterceptorHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.interceptors );
    }

    @Nonnull
    public RequestEndpoints getRequestEndpoints( @Nonnull WebRequest request )
    {
        return new RequestEndpoints( request );
    }

    private void removeEndpoint( long id, @Nonnull Collection<? extends AbstractMethodEndpointAdapter> adapters )
    {
        for( Iterator<? extends AbstractMethodEndpointAdapter> iterator = adapters.iterator(); iterator.hasNext(); )
        {
            AbstractMethodEndpointAdapter handler = iterator.next();
            if( handler.getId() == id )
            {
                iterator.remove();
                LOG.info( "Removed @{} {}", handler.getEndpoint().getType().annotationType().getSimpleName(), handler.getEndpoint() );
                return;
            }
        }
    }

    public class RequestEndpoints
    {
        @Nonnull
        private final WebRequest request;

        @Nonnull
        private final List<Pair<RequestHandlerEndpointAdapter, HandlerContext>> handlers;

        @Nonnull
        private final List<Pair<InterceptorEndpointAdapter, HandlerContext>> interceptors;

        public RequestEndpoints( @Nonnull WebRequest request )
        {
            this.request = request;

            // find best request handler
            List<Pair<RequestHandlerEndpointAdapter, HandlerContext>> handlers = null;
            for( RequestHandlerEndpointAdapter adapter : WebEndpointsManager.this.handlers )
            {
                HandlerContext context = adapter.matches( request );
                if( context != null )
                {
                    if( handlers == null )
                    {
                        handlers = new LinkedList<>();
                    }
                    handlers.add( ImmutablePair.of( adapter, context ) );
                }
            }
            if( handlers == null )
            {
                this.handlers = Collections.emptyList();
            }
            else
            {
                this.handlers = handlers;
            }

            // find matching interceptors
            List<Pair<InterceptorEndpointAdapter, HandlerContext>> interceptors = null;
            for( InterceptorEndpointAdapter adapter : WebEndpointsManager.this.interceptors )
            {
                HandlerContext context = adapter.matches( request );
                if( context != null )
                {
                    if( interceptors == null )
                    {
                        interceptors = new LinkedList<>();
                    }
                    interceptors.add( ImmutablePair.of( adapter, context ) );
                }
            }
            if( interceptors == null )
            {
                this.interceptors = Collections.emptyList();
            }
            else
            {
                this.interceptors = interceptors;
            }
        }

        @Nonnull
        public List<Pair<RequestHandlerEndpointAdapter, HandlerContext>> getHandlers()
        {
            return this.handlers;
        }

        @Nonnull
        public List<Pair<InterceptorEndpointAdapter, HandlerContext>> getInterceptors()
        {
            return this.interceptors;
        }

        @Nullable
        public Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext> findExceptionHandler(
                @Nonnull Throwable throwable )
        {
            List<Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext>> exceptionHandlers = new ArrayList<>( WebEndpointsManager.this.exceptionHandlers.size() );
            for( ExceptionHandlerEndpointAdapter exceptionHandler : WebEndpointsManager.this.exceptionHandlers )
            {
                ExceptionHandlerEndpointAdapter.ExceptionHandlerContext handlerContext = exceptionHandler.matches( this.request, throwable );
                if( handlerContext != null )
                {
                    exceptionHandlers.add( ImmutablePair.of( exceptionHandler, handlerContext ) );
                }
            }

            if( exceptionHandlers.isEmpty() )
            {
                return null;
            }

            sort( exceptionHandlers, new Comparator<Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext>>()
            {
                @Override
                public int compare( Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext> o1,
                                    Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext> o2 )
                {
                    ExceptionHandlerEndpointAdapter.ExceptionHandlerContext ctx1 = o1.getRight();
                    ExceptionHandlerEndpointAdapter.ExceptionHandlerContext ctx2 = o2.getRight();
                    if( ctx1 == null || ctx2 == null )
                    {
                        throw new IllegalStateException();
                    }

                    int d1 = ctx1.getDistance();
                    int d2 = ctx2.getDistance();
                    if( d1 < d2 )
                    {
                        return -1;
                    }
                    else if( d1 > d2 )
                    {
                        return 1;
                    }
                    else
                    {
                        ExceptionHandlerEndpointAdapter h1 = o1.getLeft();
                        ExceptionHandlerEndpointAdapter h2 = o2.getLeft();
                        if( h1 == null || h2 == null )
                        {
                            throw new IllegalStateException();
                        }
                        else
                        {
                            return h1.compareTo( h2 );
                        }
                    }
                }
            } );
            return exceptionHandlers.get( 0 );
        }
    }
}
