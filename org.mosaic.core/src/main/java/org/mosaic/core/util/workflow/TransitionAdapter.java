package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public abstract class TransitionAdapter implements TransitionListener
{
    @Override
    public void validate( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        // no-op
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        // no-op
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        // no-op
    }
}
