package org.mosaic.web.handler.impl;

import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.web.handler.spi.RequestPlanFactory;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
@Component
final class RequestPlanFactoryImpl implements RequestPlanFactory
{
    @Nonnull
    @Override
    public Runnable createRequestPlanExecutor( @Nonnull WebRequest request )
    {
        return new RequestPlan( request );
    }
}
