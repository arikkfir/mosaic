package org.mosaic.web.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.marshall.MessageMarshaller;
import org.mosaic.web.marshall.UnmarshallableContentException;
import org.mosaic.web.marshall.impl.MarshallerManager;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class RequestPlan
{
    private static final Logger LOG = LoggerFactory.getLogger( RequestPlan.class );

    @Nonnull
    private final InterceptorChain interceptorChain = new InterceptorChainImpl();

    @Nonnull
    private final WebRequest request;

    @Nonnull
    private final Iterator<InterceptorAdapter> interceptors;

    @Nonnull
    private final RequestHandler requestHandler;

    @Nonnull
    @Component
    private RequestHandlerManager requestHandlerManager;

    @Nonnull
    @Component
    private MarshallerManager marshallerManager;

    RequestPlan( @Nonnull WebRequest request )
    {
        this.request = request;
        this.requestHandler = findRequestHandler();
        this.interceptors = findInterceptors().iterator();
    }

    void execute()
    {
        // execute interceptors and handler
        Object result;
        try
        {
            result = this.interceptorChain.proceed();
        }
        catch( Throwable throwable )
        {
            handleError( throwable );
            return;
        }

        // marshall result
        if( result != null )
        {
            LOG.debug( "Marshalling result '{}'", result );
            try
            {
                this.marshallerManager.marshall( new WebRequestSink( result ), this.request.getHeaders().getAccept() );
            }
            catch( UnmarshallableContentException e )
            {
                this.request.getResponse().setStatus( HttpStatus.NOT_ACCEPTABLE );
                this.request.getResponse().disableCaching();
            }
            catch( Throwable throwable )
            {
                handleError( throwable );
            }
        }
    }

    @Nonnull
    private RequestHandler findRequestHandler()
    {
        String method = this.request.getMethod();
        for( RequestHandler handler : this.requestHandlerManager.findRequestHandlers( this.request ) )
        {
            if( handler.getHttpMethods().contains( method ) )
            {
                return handler;
            }
        }
        throw new IllegalStateException();
    }

    @Nonnull
    private List<InterceptorAdapter> findInterceptors()
    {
        String method = this.request.getMethod();

        List<InterceptorAdapter> interceptors = this.requestHandlerManager.findInterceptors( this.request, this.requestHandler );
        Iterator<InterceptorAdapter> iterator = interceptors.iterator();
        while( iterator.hasNext() )
        {
            InterceptorAdapter interceptor = iterator.next();
            if( !interceptor.getHttpMethods().contains( method ) )
            {
                iterator.remove();
            }
        }
        return interceptors;
    }

    private void handleError( Throwable throwable )
    {
        // TODO: add application-specific error handling, maybe error page, @ErrorHandler(s), etc
        WebResponse response = this.request.getResponse();
        response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
        response.disableCaching();
        this.request.dumpToErrorLog( LOG, "Request handling failed: {}", throwable.getMessage(), throwable );
    }

    private class InterceptorChainImpl implements InterceptorChain
    {
        @Nullable
        @Override
        public Object proceed() throws Throwable
        {
            if( RequestPlan.this.interceptors.hasNext() )
            {
                return RequestPlan.this.interceptors.next().handle( RequestPlan.this.request, this );
            }
            else
            {
                return RequestPlan.this.requestHandler.handle( RequestPlan.this.request );
            }
        }
    }

    private class WebRequestSink implements MessageMarshaller.MarshallingSink
    {
        @Nonnull
        private final Object value;

        private WebRequestSink( @Nonnull Object value )
        {
            this.value = value;
        }

        @Nullable
        @Override
        public MediaType getContentType()
        {
            return RequestPlan.this.request.getResponse().getHeaders().getContentType();
        }

        @Override
        public void setContentType( @Nullable MediaType mediaType )
        {
            RequestPlan.this.request.getResponse().getHeaders().setContentType( mediaType );
        }

        @Nonnull
        @Override
        public Object getValue()
        {
            return this.value;
        }

        @Nonnull
        @Override
        public OutputStream getOutputStream() throws IOException
        {
            return RequestPlan.this.request.getResponse().stream();
        }
    }
}
