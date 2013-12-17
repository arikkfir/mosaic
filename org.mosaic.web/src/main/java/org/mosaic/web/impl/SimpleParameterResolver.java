package org.mosaic.web.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;

/**
 * @author arik
 */
final class SimpleParameterResolver implements ParameterResolver
{
    @Nonnull
    private final String key;

    @Nonnull
    private final Class<?> type;

    SimpleParameterResolver( @Nonnull String key, @Nonnull Class<?> type )
    {
        this.key = key;
        this.type = type;
    }

    @Nullable
    @Override
    public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
            throws Exception
    {
        if( parameter.getType().isAssignableFrom( this.type ) )
        {
            return resolveContext.require( this.key, this.type );
        }
        return SKIP;
    }
}
