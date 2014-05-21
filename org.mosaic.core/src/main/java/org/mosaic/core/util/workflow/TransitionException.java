package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class TransitionException extends WorkflowException
{
    @Nonnull
    private final Status origin;

    @Nonnull
    private final Status target;

    public TransitionException( @Nonnull String message,
                                @Nonnull Workflow workflow,
                                @Nonnull Status origin, @Nonnull Status target )
    {
        super( message, workflow );
        this.origin = origin;
        this.target = target;
    }

    public TransitionException( @Nonnull String message,
                                @Nonnull Throwable cause,
                                @Nonnull Workflow workflow,
                                @Nonnull Status origin,
                                @Nonnull Status target )
    {
        super( message, cause, workflow );
        this.origin = origin;
        this.target = target;
    }

    @Nonnull
    public final Status getOrigin()
    {
        return this.origin;
    }

    @Nonnull
    public final Status getTarget()
    {
        return this.target;
    }
}
