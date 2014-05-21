package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class WorkflowException extends RuntimeException
{
    @Nonnull
    private final Workflow workflow;

    public WorkflowException( @Nonnull String message, @Nonnull Workflow workflow )
    {
        super( message );
        this.workflow = workflow;
    }

    public WorkflowException( @Nonnull String message, @Nonnull Throwable cause, @Nonnull Workflow workflow )
    {
        super( message, cause );
        this.workflow = workflow;
    }

    @Nonnull
    public final Workflow getWorkflow()
    {
        return this.workflow;
    }
}
