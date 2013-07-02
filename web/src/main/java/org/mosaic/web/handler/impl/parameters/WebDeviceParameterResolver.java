package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebDevice;

/**
 * @author arik
 */
public class WebDeviceParameterResolver extends CheckedParameterResolver<WebDevice>
{
    @Nullable
    @Override
    protected WebDevice resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context ).getDevice();
    }
}
