package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebPart;

/**
 * @author arik
 */
public class WebPartParameterResolver extends CheckedParameterResolver<WebPart>
{
    @Nullable
    @Override
    protected WebPart resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context ).getBody().getPart( parameter.getName() );
    }
}
