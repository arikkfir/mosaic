package org.mosaic.web.server.impl;

import com.google.common.base.Optional;
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
import org.mosaic.modules.ServiceReference;
import org.mosaic.security.Security;
import org.mosaic.security.Subject;
import org.mosaic.util.expression.Expression;
import org.mosaic.web.server.*;

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
    private final Iterator<RequestInterceptor> interceptors;

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
    private List<ServiceReference<Authenticator>> authenticators;

    @Nonnull
    @Service
    private List<ServiceReference<Challanger>> challangers;

    @Nonnull
    @Service
    private Security security;

    public RequestPlan( @Nonnull WebInvocation request )
    {
        this.request = request;

        List<RequestHandler> handlers = this.requestHandlersManager.findRequestHandlers( this.request );
        if( handlers.isEmpty() )
        {
            // TODO: should we return 404 here?
            throw new IllegalStateException( "no handlers found" );
        }
        else
        {
            this.requestHandler = handlers.get( 0 );
        }

        this.interceptors = this.requestHandlersManager.findInterceptors( this.request, this.requestHandler ).iterator();
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
                    Optional<Authentication> holder = authenticator.authenticate( this.request );
                    if( holder.isPresent() )
                    {
                        if( holder.get().getResult() == AuthenticationResult.AUTHENTICATED )
                        {
                            return holder.get().getSubject();
                        }
                    }
                    // TODO: we might want to log failed authentication attempts here
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
            Boolean result = expression.createInvocation( subject ).invoke();
            if( result == null || !result )
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

    @Nullable
    private Authenticator findAuthenticator( @Nonnull String authenticationMethod )
    {
        for( ServiceReference<Authenticator> reference : this.authenticators )
        {
            Optional<String> method = reference.getProperties().find( "method", String.class );
            if( method.isPresent() && method.get().equalsIgnoreCase( authenticationMethod ) )
            {
                return reference.service().get();
            }
        }
        return null;
    }

    @Nullable
    private Challanger findChallanger( @Nonnull String authenticationMethod )
    {
        for( ServiceReference<Challanger> reference : this.challangers )
        {
            Optional<String> method = reference.getProperties().find( "method", String.class );
            if( method.isPresent() && method.get().equalsIgnoreCase( authenticationMethod ) )
            {
                return reference.service().get();
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
        private boolean handlerInvoked;

        @Nullable
        @Override
        public Object proceed() throws Throwable
        {
            if( RequestPlan.this.interceptors.hasNext() )
            {
                return RequestPlan.this.interceptors.next().handle( RequestPlan.this.request, this );
            }
            else if( this.handlerInvoked )
            {
                throw new IllegalStateException( "double handler invocation" );
            }
            else
            {
                this.handlerInvoked = true;
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
            if( RequestPlan.this.requestHandler instanceof Secured )
            {
                Secured secured = ( Secured ) RequestPlan.this.requestHandler;
                this.handlerSecurityConstraint = secured.getSecurityConstraint( RequestPlan.this.request );
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
