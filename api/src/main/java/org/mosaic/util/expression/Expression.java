package org.mosaic.util.expression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Expression
{
    Invoker createInvoker();

    interface Invoker
    {
        InvokerWithRoot withRoot( @Nonnull Object root );
    }

    interface InvokerWithRoot
    {
        InvokerWithRoot setVariable( @Nonnull String name, @Nullable Object value );

        <T> TypedInvoker<T> expect( @Nonnull Class<T> type );
    }

    interface TypedInvoker<T>
    {
        T invoke();
    }
}
