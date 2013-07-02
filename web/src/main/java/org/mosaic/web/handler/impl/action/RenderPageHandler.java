package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class RenderPageHandler implements Handler
{
    @Override
    public void apply( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        plan.addHandler( this, context );
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebRequest request, @Nonnull MapEx<String, Object> context ) throws Exception
    {
        // TODO arik: implement RenderPageHandler.handle([plan])
        throw new UnsupportedOperationException();
    }
}
