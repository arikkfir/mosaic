package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebResponse;

/**
 * @author arik
 */
public class WebResponseParameterResolver extends CheckedParameterResolver<WebResponse>
{
    @Nullable
    @Override
    protected WebResponse resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context ).getResponse();
    }
}
