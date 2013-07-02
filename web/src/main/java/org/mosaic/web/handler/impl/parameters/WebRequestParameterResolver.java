package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class WebRequestParameterResolver extends CheckedParameterResolver<WebRequest>
{
    @Nullable
    @Override
    protected WebRequest resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        return getRequest( context );
    }
}
