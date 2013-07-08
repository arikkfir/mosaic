package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionEvaluateException;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class SecuredHandler implements Handler
{
    private static final Logger LOG = LoggerFactory.getLogger( SecuredHandler.class );

    @Nonnull
    private final Handler handler;

    @Nonnull
    private final Expression securityExpression;

    public SecuredHandler( @Nonnull Handler handler, @Nonnull Expression securityExpression )
    {
        this.handler = handler;
        this.securityExpression = securityExpression;
    }

    @Override
    public void apply( @Nonnull final RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        this.handler.apply( new RequestExecutionPlan()
        {
            @Nonnull
            @Override
            public WebRequest getRequest()
            {
                return plan.getRequest();
            }

            @Nonnull
            @Override
            public ExpressionParser getExpressionParser()
            {
                return plan.getExpressionParser();
            }

            @Override
            public void addInterceptor( @Nonnull Interceptor interceptor, @Nonnull MapEx<String, Object> context )
            {
                plan.addInterceptor( interceptor, context );
            }

            @Override
            public void addHandler( @Nonnull Handler handler, @Nonnull MapEx<String, Object> context )
            {
                plan.addHandler( SecuredHandler.this, context );
            }

            @Override
            public void addExceptionHandler( @Nonnull ExceptionHandler exceptionHandler,
                                             @Nonnull MapEx<String, Object> context )
            {
                plan.addExceptionHandler( exceptionHandler, context );
            }

            @Override
            public boolean canHandle()
            {
                return plan.canHandle();
            }

            @Override
            public void execute()
            {
                plan.execute();
            }
        }, context );
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request, @Nonnull MapEx<String, Object> context ) throws Exception
    {
        try
        {
            if( !this.securityExpression.createInvoker().withRoot( request ).expect( Boolean.class ).require() )
            {
                if( request.getUser().isAnonymous() )
                {
                    request.getResponse().setStatus( HttpStatus.UNAUTHORIZED );
                }
                else
                {
                    request.getResponse().setStatus( HttpStatus.FORBIDDEN );
                }
                return null;
            }
        }
        catch( ExpressionEvaluateException e )
        {
            LOG.warn( "Error while checking authorization for {}: {}", this.handler, e.getMessage() );
            return null;
        }
        catch( Exception e )
        {
            LOG.warn( "Error while checking authorization for {}: {}", this.handler, e.getMessage(), e );
            return null;
        }
        return this.handler.handle( request, context );
    }
}
