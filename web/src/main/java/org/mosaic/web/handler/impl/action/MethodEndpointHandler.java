package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.parameters.*;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class MethodEndpointHandler extends MethodEndpointWrapper implements Handler
{
    public MethodEndpointHandler( @Nonnull MethodEndpoint endpoint, @Nonnull ConversionService conversionService )
    {
        super( endpoint );
        addParameterResolvers(
                new CookieParameterResolver( conversionService ),
                new HeaderParameterResolver( conversionService ),
                new QueryValueParameterResolver( conversionService ),
                new WebApplicationParameterResolver(),
                new WebDeviceParameterResolver(),
                new WebPartParameterResolver(),
                new WebRequestParameterResolver(),
                new UserParameterResolver(),
                new UriValueParameterResolver( conversionService ),
                new WebResponseParameterResolver(),
                new WebSessionParameterResolver(),
                new WebRequestUriParameterResolver(),
                new WebRequestHeadersParameterResolver(),
                new WebRequestBodyParameterResolver( conversionService )
        );
    }

    @Override
    public void apply( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        plan.addHandler( this, context );
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request, @Nonnull MapEx<String, Object> context ) throws Exception
    {
        return invoke( context );
    }
}
