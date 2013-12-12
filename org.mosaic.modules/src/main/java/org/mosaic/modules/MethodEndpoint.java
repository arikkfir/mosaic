package org.mosaic.modules;

import java.lang.annotation.Annotation;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.ParameterResolver;

/**
 * @author arik
 */
public interface MethodEndpoint<T extends Annotation>
{
    @Nonnull
    String getName();

    @Nonnull
    T getType();

    @Nonnull
    MethodHandle getMethodHandle();

    @Nonnull
    Invoker createInvoker( @Nonnull ParameterResolver... resolvers );

    @Nullable
    Object invoke( @Nullable Object... args ) throws Throwable;

    interface Invoker
    {
        @Nonnull
        MethodHandle getMethod();

        @Nonnull
        Invocation resolve( @Nonnull Map<String, Object> resolveContext );
    }

    interface Invocation
    {
        @Nonnull
        Invoker getInvoker();

        @Nonnull
        Object[] getArguments();

        @Nullable
        Object invoke() throws Exception;
    }
}
