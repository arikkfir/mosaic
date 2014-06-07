package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
public final class TransitionContext
{
    @Nonnull
    private final Workflow workflow;

    @Nonnull
    private final Status origin;

    @Nonnull
    private final Status target;

    public TransitionContext( @Nonnull Workflow workflow,
                              @Nonnull Status origin,
                              @Nonnull Status target )
    {
        this.workflow = workflow;
        this.origin = origin;
        this.target = target;
    }

    @Nonnull
    public Workflow getWorkflow()
    {
        return this.workflow;
    }

    @Nonnull
    public Status getOrigin()
    {
        return this.origin;
    }

    @Nonnull
    public Status getTarget()
    {
        return this.target;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        TransitionContext that = ( TransitionContext ) o;

        if( !origin.equals( that.origin ) )
        {
            return false;
        }
        if( !target.equals( that.target ) )
        {
            return false;
        }
        if( !workflow.equals( that.workflow ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = this.workflow.hashCode();
        result = 31 * result + this.origin.hashCode();
        result = 31 * result + this.target.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "origin", this.origin )
                             .add( "target", this.target )
                             .toString();
    }
}
