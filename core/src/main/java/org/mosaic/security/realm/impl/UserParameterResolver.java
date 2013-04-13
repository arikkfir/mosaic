package org.mosaic.security.realm.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.realm.MutableUser;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public class UserParameterResolver implements MethodHandle.ParameterResolver
{
    @Nullable
    @Override
    public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
    {
        if( parameter.getType().isAssignableFrom( MutableUser.class ) )
        {
            return resolveContext.get( "user" );
        }
        else
        {
            return SKIP;
        }
    }
}
