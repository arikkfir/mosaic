package org.mosaic.web.handler.impl;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.impl.adapter.ExceptionHandlerEndpointAdapter;
import org.mosaic.web.handler.impl.adapter.HandlerContext;
import org.mosaic.web.handler.impl.adapter.InterceptorEndpointAdapter;
import org.mosaic.web.handler.impl.adapter.RequestHandlerEndpointAdapter;
import org.mosaic.web.marshall.MarshallingManager;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.net.MediaType;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
@Bean
public class ExecutionPlanFactory
{
    @Nonnull
    private WebEndpointsManager webEndpointsManager;

    @Nonnull
    private MarshallingManager marshallingManager;

    @BeanRef
    public void setWebEndpointsManager( @Nonnull WebEndpointsManager webEndpointsManager )
    {
        this.webEndpointsManager = webEndpointsManager;
    }

    @BeanRef
    public void setMarshallingManager( @Nonnull MarshallingManager marshallingManager )
    {
        this.marshallingManager = marshallingManager;
    }

    @Nonnull
    public ExecutionPlan buildExecutionPlan( @Nonnull WebRequest request )
    {
        return new ExecutionPlan( request );
    }

    public class ExecutionPlan implements InterceptorChain
    {
        @Nonnull
        private final WebRequest request;

        @Nonnull
        private final WebEndpointsManager.RequestEndpoints endpoints;

        private int nextInterceptorIndex = 0;

        public ExecutionPlan( @Nonnull WebRequest request )
        {
            this.request = request;
            this.endpoints = webEndpointsManager.getRequestEndpoints( request );
        }

        @Nullable
        @Override
        public synchronized Object next() throws Exception
        {
            if( this.nextInterceptorIndex < 0 )
            {
                return handleMultipleExecutions();
            }
            else if( this.nextInterceptorIndex == this.endpoints.getInterceptors().size() )
            {
                return invokeHandler();
            }
            else
            {
                return invokeNextInterceptor();
            }
        }

        public void execute()
        {
            Object result;
            try
            {
                result = next();
            }
            catch( Throwable handleException )
            {
                try
                {
                    Pair<ExceptionHandlerEndpointAdapter, ExceptionHandlerEndpointAdapter.ExceptionHandlerContext> handlerInfo = this.endpoints.findExceptionHandler( handleException );
                    if( handlerInfo == null )
                    {
                        this.request.dumpToLog( "no exception handler found for handler exception", handleException );
                        this.request.getResponse().setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
                        result = null;
                    }
                    else
                    {
                        ExceptionHandlerEndpointAdapter exceptionHandler = handlerInfo.getLeft();
                        ExceptionHandlerEndpointAdapter.ExceptionHandlerContext handlerContext = handlerInfo.getRight();
                        if( exceptionHandler == null || handlerContext == null )
                        {
                            throw new IllegalStateException();
                        }
                        result = exceptionHandler.handle( handlerContext );
                    }
                }
                catch( Exception exceptionHandlingException )
                {
                    this.request.dumpToLog( "no exception handler found for handler exception", exceptionHandlingException );
                    result = null;
                }
            }

            if( result != null )
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
        }

        @Nullable
        private Object invokeNextInterceptor() throws Exception
        {
            Pair<InterceptorEndpointAdapter, HandlerContext> interceptorInfo = this.endpoints.getInterceptors().get( this.nextInterceptorIndex++ );
            InterceptorEndpointAdapter interceptor = interceptorInfo.getLeft();
            HandlerContext handlerContext = interceptorInfo.getRight();
            if( interceptor == null || handlerContext == null )
            {
                throw new IllegalStateException( "Interceptor info pair with no handler - this should not happen" );
            }
            else
            {
                return interceptor.handle( handlerContext, this );
            }
        }

        @Nullable
        private Object invokeHandler() throws Exception
        {
            this.nextInterceptorIndex = -1;

            // if method is OPTIONS, just enumerate available HTTP methods and return
            if( this.request.getMethod() == HttpMethod.OPTIONS )
            {
                Set<HttpMethod> methods = new TreeSet<>();
                for( Pair<RequestHandlerEndpointAdapter, HandlerContext> handlerInfo : this.endpoints.getHandlers() )
                {
                    RequestHandlerEndpointAdapter handler = handlerInfo.getLeft();
                    HandlerContext handlerContext = handlerInfo.getRight();
                    if( handler != null && handlerContext != null )
                    {
                        if( handler.matchesHttpMethod( this.request ) )
                        {
                            methods.addAll( handler.getHttpMethods() );
                        }
                    }
                }
                this.request.getResponse().setStatus( HttpStatus.OK );
                this.request.getResponse().getHeaders().setAllow( methods );
                return null;
            }

            // find first handler matching the HTTP method
            RequestHandlerEndpointAdapter matchingHandler = null;
            HandlerContext matchingHandlerContext = null;
            for( Pair<RequestHandlerEndpointAdapter, HandlerContext> handlerInfo : this.endpoints.getHandlers() )
            {
                RequestHandlerEndpointAdapter handler = handlerInfo.getLeft();
                HandlerContext handlerContext = handlerInfo.getRight();
                if( handler == null || handlerContext == null )
                {
                    throw new IllegalStateException( "Handler info pair with no handler - this should not happen" );
                }

                if( handler.matchesHttpMethod( this.request ) )
                {
                    matchingHandler = handler;
                    matchingHandlerContext = handlerContext;
                    break;
                }
            }

            if( matchingHandler != null )
            {
                // a match was found, invoke that match
                return matchingHandler.handle( matchingHandlerContext );
            }
            else if( this.endpoints.getHandlers().isEmpty() )
            {
                // TODO arik: decide whether to render HTML page-not-found or just return 404 (Accept header, configurable, etc?)
                // no handler matched the request - return 404
                this.request.getResponse().setStatus( HttpStatus.NOT_FOUND );
                return null;
            }
            else
            {
                // some handlers matched the request - but none of them match the HTTP methods
                // e.g. request was POST but all handlers support only GET
                if( this.request.getProtocol().endsWith( "1.1" ) )
                {
                    this.request.getResponse().setStatus( HttpStatus.METHOD_NOT_ALLOWED );
                }
                else
                {
                    this.request.getResponse().setStatus( HttpStatus.BAD_REQUEST );
                }
                return null;
            }
        }

        @Nullable
        private Object handleMultipleExecutions()
        {
            throw new IllegalStateException( "Repeated request handler execution for: " + this );
        }
    }
}
