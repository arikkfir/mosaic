package org.mosaic.web.endpoint.impl;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.method.MethodParameter;
import org.mosaic.util.method.ParameterResolver;

/**
 * @author arik
 */
final class SimpleParameterResolver<T> implements ParameterResolver<T>
{
    @Nonnull
    private final String key;

    @Nonnull
    private final Class<T> type;

    SimpleParameterResolver( @Nonnull String key, @Nonnull Class<T> type )
    {
        this.key = key;
        this.type = type;
    }

    @Nullable
    @Override
    public Optional<T> resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
            throws Exception
    {
        if( parameter.getType().isAssignableFrom( this.type ) )
        {
            return resolveContext.find( this.key, this.type );
        }
        return Optional.absent();
    }
}
