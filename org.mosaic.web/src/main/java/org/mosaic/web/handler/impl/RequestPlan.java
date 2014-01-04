package org.mosaic.web.handler.impl;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.util.expression.Expression;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.SecuredRequestHandler;
import org.mosaic.web.http.HttpStatus;
import org.mosaic.web.marshall.MessageMarshaller;
import org.mosaic.web.marshall.UnmarshallableContentException;
import org.mosaic.web.marshall.impl.MarshallerManager;
import org.mosaic.web.request.WebInvocation;
import org.mosaic.web.security.Authentication;
import org.mosaic.web.security.Authenticator;
import org.mosaic.web.security.Challanger;
import org.mosaic.web.security.SecurityConstraint;

import static org.mosaic.web.security.AuthenticationResult.AUTHENTICATED;

/**
 * @author arik
 */
public final class RequestPlan implements Runnable
{
    @Nonnull
    private final InterceptorChain interceptorChain = new InterceptorChainImpl();

    @Nonnull
    private final WebInvocation request;

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
    private List<Challanger> challangers;

    @Nonnull
    @Service
    private Security security;

    public RequestPlan( @Nonnull WebInvocation request )
    {
        this.request = request;
        this.requestHandler = findRequestHandler();
        this.interceptors = findInterceptors().iterator();
    }

    @Override
    public void run()
    {
        SecurityConstraint securityConstraint = new MergedSecurityConstraint();
        Subject subject = authenticate( securityConstraint );
        if( authorize( securityConstraint, subject ) )
        {
            subject.login();
            try
            {
                // execute interceptors and handler
                try
                {
                    Object result = this.interceptorChain.proceed();
                    if( result != null )
                    {
                        List<MediaType> accept = this.request.getHttpRequest().getAccept();
                        try
                        {
                            this.marshallerManager.marshall( new WebRequestSink( result ), accept );
                        }
                        catch( UnmarshallableContentException e )
                        {
                            this.request.getHttpResponse().setStatus( HttpStatus.NOT_ACCEPTABLE, "Unable to generate " + accept );
                            this.request.disableCaching();
                        }
                    }
                }
                catch( Throwable throwable )
                {
                    handleError( throwable );
                }
            }
            finally
            {
                subject.logout();
            }
        }
    }

    @Nonnull
    private Subject authenticate( @Nonnull SecurityConstraint securityConstraint )
    {
        Collection<String> authenticationMethods = securityConstraint.getAuthenticationMethods();
        if( authenticationMethods != null )
        {
            for( String authMethod : authenticationMethods )
            {
                Authenticator authenticator = findAuthenticator( authMethod );
                if( authenticator != null )
                {
                    Authentication authentication = authenticator.authenticate( this.request );
                    if( authentication.getResult() == AUTHENTICATED )
                    {
                        return authentication.getSubject();
                    }
                }
            }
        }
        return this.security.getAnonymousSubject();
    }

    private boolean authorize( @Nonnull SecurityConstraint securityConstraint, @Nonnull Subject subject )
    {
        Expression<Boolean> expression = securityConstraint.getExpression();
        if( expression != null )
        {
            if( !expression.createInvocation( subject ).require() )
            {
                String authMethod = securityConstraint.getChallangeMethod();
                Challanger challanger = authMethod != null ? findChallanger( authMethod ) : null;
                if( challanger == null || subject.isAuthenticated() )
                {
                    // no point in challanging already-authenticated user, when no challange auth method is defined
                    this.request.getHttpResponse().setStatus( HttpStatus.FORBIDDEN, "Access denied" );
                }
                else
                {
                    // no authenticated user, and we have the challange auth method - send the challange
                    challanger.challange( this.request );
                }
                return false;
            }
        }
        return true;
    }

    @Nonnull
    private RequestHandler findRequestHandler()
    {
        String method = this.request.getHttpRequest().getMethod();
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
        String method = this.request.getHttpRequest().getMethod();

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

    @Nullable
    private Authenticator findAuthenticator( @Nonnull String authenticationMethod )
    {
        for( Authenticator authenticator : this.authenticators )
        {
            if( authenticator.getAuthenticationMethod().equalsIgnoreCase( authenticationMethod ) )
            {
                return authenticator;
            }
        }
        return null;
    }

    @Nullable
    private Challanger findChallanger( @Nonnull String authenticationMethod )
    {
        for( Challanger challanger : this.challangers )
        {
            if( challanger.getAuthenticationMethod().equalsIgnoreCase( authenticationMethod ) )
            {
                return challanger;
            }
        }
        return null;
    }

    private void handleError( @Nonnull Throwable throwable )
    {
        // TODO: add application-specific error handling, maybe error page, @ErrorHandler(s), etc
        this.request.getHttpResponse().setStatus( HttpStatus.INTERNAL_SERVER_ERROR, "Internal error" );
        this.request.disableCaching();
        this.request.getHttpLogger().error( "Request handling failed: {}", throwable.getMessage(), throwable );
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
            return RequestPlan.this.request.getHttpResponse().getContentType();
        }

        @Override
        public void setContentType( @Nullable MediaType mediaType )
        {
            RequestPlan.this.request.getHttpResponse().setContentType( mediaType );
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
            return RequestPlan.this.request.getHttpResponse().getOutputStream();
        }
    }

    private class MergedSecurityConstraint implements SecurityConstraint
    {
        @Nullable
        private final SecurityConstraint handlerSecurityConstraint;

        @Nullable
        private final SecurityConstraint requestSecurityConstraint;

        private MergedSecurityConstraint()
        {
            if( RequestPlan.this.requestHandler instanceof SecuredRequestHandler )
            {
                SecuredRequestHandler securedRequestHandler = ( SecuredRequestHandler ) RequestPlan.this.requestHandler;
                this.handlerSecurityConstraint = securedRequestHandler.getSecurityConstraint( RequestPlan.this.request );
            }
            else
            {
                this.handlerSecurityConstraint = null;
            }

            this.requestSecurityConstraint = RequestPlan.this.request.getSecurityConstraint();
        }

        @Nullable
        @Override
        public Collection<String> getAuthenticationMethods()
        {
            if( this.handlerSecurityConstraint != null )
            {
                Collection<String> authenticationMethods = this.handlerSecurityConstraint.getAuthenticationMethods();
                if( authenticationMethods != null )
                {
                    return authenticationMethods;
                }
            }
            return this.requestSecurityConstraint != null ? this.requestSecurityConstraint.getAuthenticationMethods() : null;
        }

        @Nullable
        @Override
        public Expression<Boolean> getExpression()
        {
            if( this.handlerSecurityConstraint != null )
            {
                Expression<Boolean> expression = this.handlerSecurityConstraint.getExpression();
                if( expression != null )
                {
                    return expression;
                }
            }
            return this.requestSecurityConstraint != null ? this.requestSecurityConstraint.getExpression() : null;
        }

        @Nullable
        @Override
        public String getChallangeMethod()
        {
            if( this.handlerSecurityConstraint != null )
            {
                String challangeMethod = this.handlerSecurityConstraint.getChallangeMethod();
                if( challangeMethod != null )
                {
                    return challangeMethod;
                }
            }
            return this.requestSecurityConstraint != null ? this.requestSecurityConstraint.getChallangeMethod() : null;
        }
    }
}
