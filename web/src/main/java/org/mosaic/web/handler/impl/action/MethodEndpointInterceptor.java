package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.parameters.*;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class MethodEndpointInterceptor extends MethodEndpointHandler implements Interceptor
{
    public MethodEndpointInterceptor( @Nonnull MethodEndpoint endpoint, @Nonnull ConversionService conversionService )
    {
        super( endpoint, conversionService );
        addParameterResolvers(
                new CookieParameterResolver( conversionService ),
                new HeaderParameterResolver( conversionService ),
                new InterceptorChainParameterResolver(),
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
        plan.addInterceptor( this, context );
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request,
                          @Nonnull InterceptorChain interceptorChain,
                          @Nonnull MapEx<String, Object> context ) throws Exception
    {
        context.put( "interceptorChain", interceptorChain );
        return invoke( context );
    }
}
