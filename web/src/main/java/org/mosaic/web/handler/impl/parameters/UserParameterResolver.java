package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.security.User;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;

/**
 * @author arik
 */
public class UserParameterResolver extends CheckedParameterResolver<User>
{
    @Nullable
    @Override
    protected User resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context ).getUser();
    }
}
