package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface TransitionChangeAction
{
    void execute( @Nonnull TransitionContext context ) throws Exception;
}
