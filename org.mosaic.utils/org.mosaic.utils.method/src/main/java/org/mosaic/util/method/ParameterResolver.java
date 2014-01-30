package org.mosaic.util.method;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
