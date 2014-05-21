package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface TransitionListener
{
    void validate( @Nonnull Status origin, @Nonnull Status target ) throws Exception;

    void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception;

    void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception;
}
