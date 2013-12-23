package org.mosaic.web.handler.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.web.application.Application;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.marshall.MessageMarshaller;
import org.mosaic.web.marshall.UnmarshallableContentException;
import org.mosaic.web.marshall.impl.MarshallerManager;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.mosaic.web.security.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.web.security.Authenticator.AuthenticationResult.AUTHENTICATED;
import static org.mosaic.web.security.Authenticator.AuthenticationResult.AUTHENTICATION_FAILED;

/**
 * @author arik
 */
final class RequestPlan implements Runnable
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
    private RequestHandlersManagerImpl requestHandlersManager;

    @Nonnull
    @Component
    private MarshallerManager marshallerManager;

    @Nonnull
    @Service
    private List<Authenticator> authenticators;

    @Nonnull
    @Service
    private Security security;

    RequestPlan( @Nonnull WebRequest request )
    {
        this.request = request;
        this.requestHandler = findRequestHandler();
        this.interceptors = findInterceptors().iterator();
    }

    @Override
    public void run()
    {
        Application.ApplicationSecurity.SecurityConstraint securityConstraint = this.request.getSecurityConstraint();
        if( securityConstraint != null )
        {
            //
            // attempt to find appropriate authenticator for this resource
            // if found, use it for authentication:
            //      if successful, use its authenticated subject
            //      otherwise, have authenticator send a challange to client and DONT process request
            //

            String authenticationMethod = securityConstraint.getAuthenticationMethod();
            for( Authenticator authenticator : this.authenticators )
            {
                if( authenticator.getAuthenticationMethods().contains( authenticationMethod ) )
                {
                    Authenticator.AuthenticationAction action = authenticator.authenticate( this.request );
                    if( action.getResult() == AUTHENTICATED )
                    {
                        executeWithSubject( action.getSubject() );
                    }
                    else
                    {
                        authenticator.challange( this.request );
                    }
                    return;
                }
            }

            this.request.getResponse().setStatus( HttpStatus.FORBIDDEN );
            this.request.dumpToWarnLog( LOG, "Unknown authentication method '{}'", authenticationMethod );
        }
        else
        {
            //
            // resource is not protected
            // attempt all authenticators - if one succeeds, use its subject
            // if one fails - use it to send challange and fail request
            //

            Subject subject = this.security.getAnonymousSubject();
            for( Authenticator authenticator : this.authenticators )
            {
                Authenticator.AuthenticationAction action = authenticator.authenticate( this.request );
                if( action.getResult() == AUTHENTICATED )
                {
                    subject = action.getSubject();
                    break;
                }
                else if( action.getResult() == AUTHENTICATION_FAILED )
                {
                    authenticator.challange( this.request );
                    return;
                }
            }
            executeWithSubject( subject );
        }
    }

    private void executeWithSubject( @Nonnull Subject subject )
    {
        subject.login();
        try
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
        finally
        {
            subject.logout();
        }
    }

    @Nonnull
    private RequestHandler findRequestHandler()
    {
        String method = this.request.getMethod();
        for( RequestHandler handler : this.requestHandlersManager.findRequestHandlers( this.request ) )
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

        List<InterceptorAdapter> interceptors = this.requestHandlersManager.findInterceptors( this.request, this.requestHandler );
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
