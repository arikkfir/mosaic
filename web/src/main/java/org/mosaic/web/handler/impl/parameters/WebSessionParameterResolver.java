package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebSession;

/**
 * @author arik
 */
public class WebSessionParameterResolver extends CheckedParameterResolver<WebSession>
{
    @Nullable
    @Override
    protected WebSession resolveToType( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> context )
            throws Exception
    {
        if( parameter.hasAnnotation( Nonnull.class ) )
        {
            return getRequest( context ).getOrCreateSession();
        }
        else
        {
            return getRequest( context ).getSession();
        }
    }
}
