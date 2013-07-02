package org.mosaic.web.handler.impl.filter;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestExecutionPlan;

/**
 * @author arik
 */
public interface Filter
{
    boolean matches( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context );
}
