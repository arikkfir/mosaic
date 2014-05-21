package org.mosaic.util.method;

import java.util.Collection;
import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface InvokableMethodHandle extends MethodHandle
{
    @Nonnull
    Invoker createInvoker( @Nonnull ParameterResolver<?>... resolvers );

    @Nonnull
    Invoker createInvoker( @Nonnull Collection<ParameterResolver<?>> resolvers );

    @Nullable
    Object invoke( @Nullable Object bean, @Nullable Object... args ) throws Exception;

    interface Invoker
    {
        @Nonnull
        MethodHandle getMethod();

        @Nonnull
        Invocation resolve( @Nonnull Map<String, Object> context );
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
