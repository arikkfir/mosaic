package org.mosaic.core.util.workflow;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
public class Status
{
    @Nonnull
    private final String name;

    public Status( @Nonnull String name )
    {
        this.name = name;
    }

    @Nonnull
    public final String getName()
    {
        return this.name;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public final boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Status status = ( Status ) o;

        if( !this.name.equals( status.name ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public final int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public final String toString()
    {
        return ToStringHelper.create( this ).add( "name", this.name ).toString();
    }
}
