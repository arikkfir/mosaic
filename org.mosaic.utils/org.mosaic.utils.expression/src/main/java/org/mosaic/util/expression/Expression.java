package org.mosaic.util.expression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Expression<T>
{
    @Nonnull
    Invoker<T> createInvocation( @Nonnull Object root );

    interface Invoker<T>
    {
        @Nonnull
        Invoker<T> setVariable( @Nonnull String name, @Nullable Object value );

        @Nullable
        T invoke();
    }
}
