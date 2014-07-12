package org.mosaic.core.services.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.mosaic.core.modules.ServiceProperty;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author arik
 */
class FilterBuilder
{
    @Nullable
    static Filter createFilter( @Nonnull ServiceProperty... properties )
    {
        if( properties.length == 0 )
        {
            return null;
        }

        FilterBuilder filterBuilder = new FilterBuilder();
        for( ServiceProperty property : properties )
        {
            filterBuilder.addEquals( property.getName(), Objects.toString( property.getValue(), "" ) );
        }
        return filterBuilder.toFilter();
    }

    @Nonnull
    private final List<String> filters = new LinkedList<>();

    @Nonnull
    String toFilterString()
    {
        if( this.filters.isEmpty() )
        {
            return "";
        }
        else if( this.filters.size() == 1 )
        {
            return this.filters.get( 0 );
        }
        else
        {
            @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
            StringBuilder buf = new StringBuilder( 200 );
            this.filters.forEach( buf::append );
            return "(&" + buf + ")";
        }
    }

    FilterBuilder addEquals( @Nullable String key, @Nullable String value )
    {
        if( key != null )
        {
            key = key.trim();
            if( !key.isEmpty() )
            {
                if( value == null )
                {
                    value = "null";
                }
                value = value.trim();
                add( key + "=" + value );
            }
        }
        return this;
    }

    FilterBuilder add( @Nullable String filter )
    {
        if( filter != null )
        {
            filter = filter.trim();
            if( !filter.isEmpty() )
            {
                if( !filter.startsWith( "(" ) )
                {
                    filter = "(" + filter + ")";
                }
                this.filters.add( filter );
            }
        }
        return this;
    }

    @Nonnull
    Filter toFilter()
    {
        String filter = toFilterString();
        try
        {
            return FrameworkUtil.createFilter( filter );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalArgumentException( "illegal filter built: " + filter, e );
        }
    }
}
