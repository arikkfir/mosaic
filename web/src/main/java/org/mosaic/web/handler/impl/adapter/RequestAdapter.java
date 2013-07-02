package org.mosaic.web.handler.impl.adapter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.impl.RequestExecutionPlan;
import org.mosaic.web.handler.impl.action.Participator;
import org.mosaic.web.handler.impl.filter.Filter;

import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
public class RequestAdapter implements Comparable<RequestAdapter>
{
    @Nonnull
    private final ConversionService conversionService;

    private final long id;

    private final int rank;

    @Nonnull
    private final List<Filter> filters;

    @Nonnull
    private final Participator Participator;

    public RequestAdapter( @Nonnull ConversionService conversionService,
                           long id,
                           int rank,
                           @Nonnull Participator participator,
                           @Nonnull Collection<Filter> filters )
    {
        this.conversionService = conversionService;
        this.id = id;
        this.rank = rank;
        this.Participator = participator;
        this.filters = unmodifiableList( new LinkedList<>( filters ) );
    }

    public final long getId()
    {
        return id;
    }

    public final int getRank()
    {
        return rank;
    }

    @Override
    public int compareTo( @Nullable RequestAdapter o )
    {
        if( o == null )
        {
            return -1;
        }
        else if( getId() == o.getId() )
        {
            return 0;
        }
        else if( getRank() > o.getRank() )
        {
            return -1;
        }
        else if( getRank() < o.getRank() )
        {
            return 1;
        }
        else if( getId() < o.getId() )
        {
            return -1;
        }
        else if( getId() > o.getId() )
        {
            return 1;
        }
        throw new IllegalStateException( "should not happen" );
    }

    public final void apply( @Nonnull RequestExecutionPlan plan )
    {
        MapEx<String, Object> context = new HashMapEx<>( 50, this.conversionService );
        for( Filter filter : this.filters )
        {
            if( !filter.matches( plan, context ) )
            {
                return;
            }
        }
        this.Participator.apply( plan, context );
    }
}
