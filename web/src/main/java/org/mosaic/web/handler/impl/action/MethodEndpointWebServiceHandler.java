package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class MethodEndpointWebServiceHandler extends MethodEndpointHandler
{
    public MethodEndpointWebServiceHandler( @Nonnull MethodEndpoint endpoint,
                                            @Nonnull ConversionService conversionService )
    {
        super( endpoint, conversionService );
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request, @Nonnull MapEx<String, Object> context ) throws Exception
    {
        // TODO arik: wrap super result with service response wrapper object
        return super.handle( request, context );
    }
}
