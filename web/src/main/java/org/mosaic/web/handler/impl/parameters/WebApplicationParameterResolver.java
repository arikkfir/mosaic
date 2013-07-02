package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.application.WebApplication;

/**
 * @author arik
 */
public class WebApplicationParameterResolver extends CheckedParameterResolver<WebApplication>
{
    @Nullable
    @Override
    protected WebApplication resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context ).getApplication();
    }
}
