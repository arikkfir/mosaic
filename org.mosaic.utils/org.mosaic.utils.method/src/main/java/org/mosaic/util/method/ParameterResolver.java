package org.mosaic.util.method;

import com.google.common.base.Optional;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface ParameterResolver<T>
{
    @Nullable
    Optional<? extends T> resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception;
}
