package org.mosaic.util.reflection;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MethodHandleFactory
{
    @Nonnull
    MethodHandle findMethodHandle( @Nonnull Method method );

    @Nullable
    MethodHandle findMethodHandle( @Nonnull Class<?> clazz,
                                   @Nonnull String methodName,
                                   @Nonnull Class<?>... argumentTypes );
}
