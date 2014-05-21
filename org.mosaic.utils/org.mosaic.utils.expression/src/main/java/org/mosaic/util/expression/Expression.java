package org.mosaic.util.expression;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

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
