package org.mosaic.web.handler.impl;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.annotation.*;
import org.mosaic.web.handler.impl.action.ExceptionHandler;
import org.mosaic.web.handler.impl.action.*;
import org.mosaic.web.handler.impl.action.Interceptor;
import org.mosaic.web.handler.impl.adapter.*;
import org.mosaic.web.handler.impl.filter.Filter;
import org.mosaic.web.handler.impl.filter.HttpMethodFilter;
import org.mosaic.web.handler.impl.filter.PathFilter;
import org.mosaic.web.handler.impl.filter.WebApplicationFilter;
import org.mosaic.web.marshall.MarshallingManager;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.web.net.HttpMethod.GET;
import static org.mosaic.web.net.HttpMethod.POST;

/**
 * @author arik
 */
@Bean
public class WebEndpointsManager
{
    private static final Logger LOG = LoggerFactory.getLogger( WebEndpointsManager.class );

    @Nonnull
    private final Collection<HandlerAdapter> handlerAdapters = new ConcurrentSkipListSet<>();

    @Nonnull
    private final Collection<InterceptorAdapter> interceptorAdapters = new ConcurrentSkipListSet<>();

    @Nonnull
    private final Collection<ExceptionHandlerAdapter> exceptionHandlerAdapters = new ConcurrentSkipListSet<>();

    @Nonnull
    private PageAdapter pageAdapter;

    @Nonnull
    private ExpressionParser expressionParser;

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private MarshallingManager marshallingManager;

    @BeanRef
    public void setMarshallingManager( @Nonnull MarshallingManager marshallingManager )
    {
        this.marshallingManager = marshallingManager;
    }

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

    @PostConstruct
    public void init()
    {
        this.pageAdapter = new PageAdapter( this.conversionService );
    }

    @MethodEndpointBind(Controller.class)
    public void addController( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Handler handler = new MethodEndpointHandler( endpoint, this.conversionService );

        Secured securedAnn = endpoint.getAnnotation( Secured.class );
        if( securedAnn != null )
        {
            handler = new SecuredHandler( handler, securedAnn.value() );
        }

        this.handlerAdapters.add(
                new HandlerAdapter( this.conversionService,
                                    id,
                                    rank,
                                    handler,
                                    createHandlerFilters( endpoint, false ) ) );
        LOG.info( "Added @Controller {}", endpoint );
    }

    @MethodEndpointUnbind(Controller.class)
    public void removeController( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.handlerAdapters );
    }

    @MethodEndpointBind(WebService.class)
    public void addWebService( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Handler handler = new MethodEndpointWebServiceHandler( endpoint, this.conversionService );

        Secured securedAnn = endpoint.getAnnotation( Secured.class );
        if( securedAnn != null )
        {
            handler = new SecuredHandler( handler, securedAnn.value() );
        }

        this.handlerAdapters.add(
                new HandlerAdapter( this.conversionService,
                                    id,
                                    rank,
                                    handler,
                                    createHandlerFilters( endpoint, false ) ) );
        LOG.info( "Added @WebService {}", endpoint );
    }

    @MethodEndpointUnbind(WebService.class)
    public void removeWebService( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.handlerAdapters );
    }

    @MethodEndpointBind(org.mosaic.web.handler.annotation.Interceptor.class)
    public void addInterceptorHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.interceptorAdapters.add(
                new InterceptorAdapter( this.conversionService,
                                        id,
                                        rank,
                                        new MethodEndpointInterceptor( endpoint, this.conversionService ),
                                        createHandlerFilters( endpoint, false ) ) );
        LOG.info( "Added @Interceptor {}", endpoint );
    }

    @MethodEndpointUnbind(org.mosaic.web.handler.annotation.Interceptor.class)
    public void removeInterceptorHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.interceptorAdapters );
    }

    @MethodEndpointBind(org.mosaic.web.handler.annotation.ExceptionHandler.class)
    public void addExceptionHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id, @Rank int rank )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        this.exceptionHandlerAdapters.add(
                new ExceptionHandlerAdapter( this.conversionService,
                                             id,
                                             rank,
                                             new MethodEndpointExceptionHandler( endpoint, this.conversionService ),
                                             createHandlerFilters( endpoint, true ) ) );
        LOG.info( "Added @ExceptionHandler {}", endpoint );
    }

    @MethodEndpointUnbind(org.mosaic.web.handler.annotation.ExceptionHandler.class)
    public void removeExceptionHandler( @Nonnull MethodEndpoint endpoint, @ServiceId long id )
    {
        removeEndpoint( id, this.exceptionHandlerAdapters );
    }

    @Nonnull
    public RequestExecutionPlan createRequestExecutionPlan( @Nonnull WebRequest request )
    {
        return new RequestExecutionPlanImpl( request );
    }

    private List<Filter> createHandlerFilters( MethodEndpoint endpoint, boolean emptyPathListMatchesAll )
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        List<Filter> filters = new LinkedList<>();
        applyWebAppFilter( endpoint, filters );
        applyHttpMethodFilter( endpoint, filters );
        filters.add( new PathFilter( endpoint, emptyPathListMatchesAll ) );
        return filters;
    }

    private void applyHttpMethodFilter( MethodEndpoint endpoint, List<Filter> filters )
    {
        Method methodAnn = endpoint.getAnnotation( Method.class );
        if( methodAnn != null )
        {
            filters.add( new HttpMethodFilter( methodAnn.value() ) );
        }
        else
        {
            filters.add( new HttpMethodFilter( GET, POST ) );
        }
    }

    private void applyWebAppFilter( MethodEndpoint endpoint, List<Filter> filters )
    {
        WebAppFilter webAppFilterAnn = endpoint.getAnnotation( WebAppFilter.class );
        if( webAppFilterAnn != null )
        {
            filters.add( new WebApplicationFilter( webAppFilterAnn.value() ) );
        }
    }

    private void removeEndpoint( long id, @Nonnull Collection<? extends RequestAdapter> adapters )
    {
        for( Iterator<? extends RequestAdapter> iterator = adapters.iterator(); iterator.hasNext(); )
        {
            RequestAdapter handler = iterator.next();
            if( handler.getId() == id )
            {
                iterator.remove();
                return;
            }
        }
    }

    private class RequestExecutionPlanImpl implements RequestExecutionPlan, InterceptorChain
    {
        @Nonnull
        private final WebRequest request;

        @Nonnull
        private final Map<? super Participator, MapEx<String, Object>> participatorContexts = new HashMap<>();

        @Nonnull
        private final List<Interceptor> interceptors = new LinkedList<>();

        @Nonnull
        private final List<Handler> handlers = new LinkedList<>();

        @Nonnull
        private final List<ExceptionHandler> exceptionHandlers = new LinkedList<>();

        private int nextInterceptorIndex = 0;

        private RequestExecutionPlanImpl( @Nonnull WebRequest request )
        {
            this.request = request;

            for( InterceptorAdapter interceptor : WebEndpointsManager.this.interceptorAdapters )
            {
                interceptor.apply( this );
            }

            for( HandlerAdapter handler : WebEndpointsManager.this.handlerAdapters )
            {
                handler.apply( this );
            }
            WebEndpointsManager.this.pageAdapter.apply( this );

            for( ExceptionHandlerAdapter handler : WebEndpointsManager.this.exceptionHandlerAdapters )
            {
                handler.apply( this );
            }
        }

        @Nonnull
        @Override
        public WebRequest getRequest()
        {
            return this.request;
        }

        @Nonnull
        @Override
        public ExpressionParser getExpressionParser()
        {
            return WebEndpointsManager.this.expressionParser;
        }

        @Override
        public void addInterceptor( @Nonnull Interceptor interceptor,
                                    @Nonnull MapEx<String, Object> context )
        {
            context.put( "request", this.request );
            context.put( "plan", this );
            this.participatorContexts.put( interceptor, context );
            this.interceptors.add( interceptor );
        }

        @Override
        public void addHandler( @Nonnull Handler handler, @Nonnull MapEx<String, Object> context )
        {
            context.put( "request", this.request );
            context.put( "plan", this );
            this.participatorContexts.put( handler, context );
            this.handlers.add( handler );
        }

        @Override
        public void addExceptionHandler( @Nonnull ExceptionHandler exceptionHandler,
                                         @Nonnull MapEx<String, Object> context )
        {
            context.put( "request", this.request );
            context.put( "plan", this );
            this.participatorContexts.put( exceptionHandler, context );
            this.exceptionHandlers.add( exceptionHandler );
        }

        @Override
        public boolean canHandle()
        {
            return this.handlers.size() > 0;
        }

        @Override
        public void execute()
        {
            Object result;
            try
            {
                result = next();
            }
            catch( Throwable handleException )
            {
                result = handleException( handleException );
            }

            if( result != null )
            {
                marshallResult( result );
            }
        }

        @Nullable
        @Override
        public synchronized Object next() throws Exception
        {
            if( this.nextInterceptorIndex < 0 )
            {
                throw new IllegalStateException( "Repeated request handler execution for: " + this );
            }
            else if( this.nextInterceptorIndex < this.interceptors.size() )
            {
                Interceptor interceptor = this.interceptors.get( this.nextInterceptorIndex++ );
                return interceptor.handle( this.request, this, this.participatorContexts.get( interceptor ) );
            }
            else
            {
                return invokeHandler();
            }
        }

        @Nullable
        private Object invokeHandler() throws Exception
        {
            this.nextInterceptorIndex = -1;
            if( this.handlers.isEmpty() )
            {
                // no handler matched the request - return 404
                this.request.getResponse().setStatus( HttpStatus.NOT_FOUND );
                return null;
            }
            else
            {
                Handler handler = this.handlers.get( 0 );
                return handler.handle( this.request, this.participatorContexts.get( handler ) );
            }
        }

        private void marshallResult( @Nonnull Object result )
        {
            try
            {
                List<MediaType> acceptableMediaTypes = this.request.getHeaders().getAccept();
                MediaType[] acceptableMediaTypesArray = acceptableMediaTypes.toArray( new MediaType[ acceptableMediaTypes.size() ] );
                OutputStream targetOutputStream = this.request.getResponse().getBinaryBody();
                marshallingManager.marshall( result, targetOutputStream, acceptableMediaTypesArray );
            }
            catch( Exception e )
            {
                this.request.dumpToLog( "could not marshall '{}' to response: {}", result, e.getMessage(), e );
                this.request.getResponse().setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
            }
        }

        @Nullable
        private Object handleException( @Nonnull Throwable throwable )
        {
            Pair<ExceptionHandler, Integer> handlerClosestToThrowableType = null;
            for( ExceptionHandler exceptionHandler : this.exceptionHandlers )
            {
                int distance = exceptionHandler.getDistance( throwable );
                if( distance >= 0 )
                {
                    if( handlerClosestToThrowableType == null || distance < handlerClosestToThrowableType.getRight() )
                    {
                        handlerClosestToThrowableType = ImmutablePair.of( exceptionHandler, distance );
                    }
                }
            }

            if( handlerClosestToThrowableType == null )
            {
                this.request.dumpToLog( "no exception handler found for handler exception", throwable );
                this.request.getResponse().setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
                return null;
            }
            else
            {
                try
                {
                    ExceptionHandler exceptionHandler = handlerClosestToThrowableType.getLeft();
                    MapEx<String, Object> context = this.participatorContexts.get( exceptionHandler );
                    return exceptionHandler.handle( this.request, throwable, context );
                }
                catch( Exception exceptionHandlingException )
                {
                    this.request.dumpToLog( "no exception handler found for handler exception", exceptionHandlingException );
                    return null;
                }
            }
        }
    }
}
