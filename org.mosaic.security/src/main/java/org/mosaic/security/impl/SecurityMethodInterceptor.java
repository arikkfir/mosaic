package org.mosaic.security.impl;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.modules.spi.MethodInterceptor;
import org.mosaic.security.AccessDeniedException;
import org.mosaic.security.Secured;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParser;

/**
 * @author arik
 */
@Service
final class SecurityMethodInterceptor implements MethodInterceptor
{
    @Nonnull
    @Service
    private ExpressionParser expressionParser;

    @Nonnull
    @Component
    private SecurityImpl security;

    @Override
    public boolean interestedIn( @Nonnull Method method, @Nonnull MapEx<String, Object> context )
    {
        Secured securedAnn = method.getAnnotation( Secured.class );
        if( securedAnn != null )
        {
            context.put( "expression", this.expressionParser.parseExpression(
                    securedAnn.value().trim().isEmpty() ? "subject.authenticated" : securedAnn.value().trim(),
                    Boolean.class,
                    method.getDeclaringClass() ) );
            context.put( "authMethod", securedAnn.authMethod() );
            return true;
        }
        else
        {
            return false;
        }
    }

    @Nullable
    @Override
    public BeforeInvocationDecision beforeInvocation( @Nonnull BeforeMethodInvocation invocation )
            throws Throwable
    {
        MapEx<String, Object> context = invocation.getInterceptorContext();

        @SuppressWarnings( "unchecked" )
        Expression<Boolean> expr = context.require( "expression", Expression.class );
        if( expr.createInvocation( this.security.getSubject() ).require() )
        {
            return invocation.continueInvocation();
        }
        else
        {
            String authMethod = context.require( "authMethod", String.class );
            throw new AccessDeniedException( "Access denied for '" + invocation.getMethod().getName() + "'", authMethod );
        }
    }

    @Nullable
    @Override
    public Object afterInvocation( @Nonnull AfterMethodInvocation invocation ) throws Throwable
    {
        return invocation.getReturnValue();
    }

    @Nullable
    @Override
    public Object afterThrowable( @Nonnull AfterMethodException invocation ) throws Throwable
    {
        throw invocation.getThrowable();
    }
}
