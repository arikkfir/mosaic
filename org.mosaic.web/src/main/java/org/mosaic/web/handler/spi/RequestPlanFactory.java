package org.mosaic.web.handler.spi;

import javax.annotation.Nonnull;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface RequestPlanFactory
{
    @Nonnull
    Runnable createRequestPlanExecutor( @Nonnull WebRequest request );
}
