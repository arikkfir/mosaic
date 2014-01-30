package org.mosaic.util.expression;

import com.google.common.base.Optional;
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

        @Nonnull
        Optional<T> invoke();
    }
}
