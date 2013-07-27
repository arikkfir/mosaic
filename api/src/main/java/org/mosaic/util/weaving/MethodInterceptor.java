package org.mosaic.util.weaving;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public interface MethodInterceptor
{
    @Nullable
    Object intercept( @Nonnull MethodInvocation invocation ) throws Throwable;

    interface MethodInvocation
    {
        @Nonnull
        MethodHandle getMethodHandle();

        @Nonnull
        Object getObject();

        @Nonnull
        Object[] getArguments();

        @Nullable
        Object proceed() throws Throwable;

        @Nullable
        Object proceed( @Nonnull Object[] arguments ) throws Throwable;
    }
}
