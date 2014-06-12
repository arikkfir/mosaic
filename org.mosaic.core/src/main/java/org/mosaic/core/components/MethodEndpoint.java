package org.mosaic.core.components;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

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
    Method getMethod();

    @Nullable
    Object invoke( @Nullable Object... args ) throws Throwable;
}
