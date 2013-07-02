package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestExecutionPlan;

/**
 * @author arik
 */
public interface Participator
{
    void apply( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context );
}
