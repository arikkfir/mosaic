package org.mosaic.core.weaving;

import java.lang.reflect.Method;
import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface MethodInterceptor
{
    boolean interestedIn( @Nonnull Method method, @Nonnull Map<String, Object> context );

    @Nullable
    BeforeInvocationDecision beforeInvocation( @Nonnull BeforeMethodInvocation invocation ) throws Throwable;

    @Nullable
    Object afterInvocation( @Nonnull AfterMethodInvocation invocation ) throws Throwable;

    @Nullable
    Object afterThrowable( @Nonnull AfterMethodException invocation ) throws Throwable;

    interface MethodInvocation
    {
        @Nonnull
        Map<String, Object> getInterceptorContext();

        @Nonnull
        Map<String, Object> getInvocationContext();

        @Nonnull
        Method getMethod();

        @Nullable
        Object getObject();

        @Nonnull
        Object[] getArguments();
    }

    interface BeforeMethodInvocation extends MethodInvocation
    {
        @Nonnull
        BeforeInvocationDecision continueInvocation();

        @Nonnull
        BeforeInvocationDecision abort( @Nullable Object returnValue );
    }

    interface BeforeInvocationDecision
    {
    }

    interface AfterMethodInvocation extends MethodInvocation
    {
        @Nullable
        Object getReturnValue();
    }

    interface AfterMethodException extends MethodInvocation
    {
        @Nonnull
        Throwable getThrowable();
    }
}
