package org.mosaic.modules.spi;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface MethodInterceptor
{
    boolean interestedIn( @Nonnull Method method, @Nonnull MapEx<String, Object> context );

    @Nullable
    BeforeInvocationDecision beforeInvocation( @Nonnull BeforeMethodInvocation invocation ) throws Throwable;

    @Nullable
    Object afterInvocation( @Nonnull AfterMethodInvocation invocation ) throws Throwable;

    @Nullable
    Object afterThrowable( @Nonnull AfterMethodException invocation ) throws Throwable;

    interface MethodInvocation
    {
        @Nonnull
        MapEx<String, Object> getInterceptorContext();

        @Nonnull
        MapEx<String, Object> getInvocationContext();

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
