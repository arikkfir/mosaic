package org.mosaic.util.expression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Expression
{
    @Nonnull
    Invoker createInvoker();

    interface Invoker
    {
        @Nonnull
        InvokerWithRoot withRoot( @Nonnull Object root );
    }

    interface InvokerWithRoot
    {
        @Nonnull
        InvokerWithRoot setVariable( @Nonnull String name, @Nullable Object value );

        @Nonnull
        <T> TypedInvoker<T> expect( @Nonnull Class<T> type );
    }

    interface TypedInvoker<T>
    {
        @Nullable
        T get();

        @Nonnull
        T require();
    }
}
