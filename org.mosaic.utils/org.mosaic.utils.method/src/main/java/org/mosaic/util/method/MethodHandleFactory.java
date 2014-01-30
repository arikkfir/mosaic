package org.mosaic.util.method;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MethodHandleFactory
{
    @Nonnull
    InvokableMethodHandle findMethodHandle( @Nonnull Method method );

    @Nullable
    InvokableMethodHandle findMethodHandle( @Nonnull Class<?> clazz,
                                            @Nonnull String methodName,
                                            @Nonnull Class<?>... argumentTypes );
}
