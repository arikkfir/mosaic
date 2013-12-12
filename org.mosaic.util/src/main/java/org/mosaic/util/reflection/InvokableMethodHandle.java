package org.mosaic.util.reflection;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface InvokableMethodHandle extends MethodHandle
{
    @Nonnull
    Invoker createInvoker( @Nonnull ParameterResolver... resolvers );

    @Nonnull
    Invoker createInvoker( @Nonnull Collection<ParameterResolver> resolvers );

    @Nullable
    Object invoke( @Nullable Object bean, @Nullable Object... args ) throws Exception;

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
        Object invoke( @Nonnull Object bean ) throws Exception;
    }
}
