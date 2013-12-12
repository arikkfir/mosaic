package org.mosaic.util.reflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface ParameterResolver
{
    @Nonnull
    Object SKIP = new Object();

    @Nullable
    Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
            throws Exception;
}
