package org.mosaic.util.osgi;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author arik
 */
public class FilterBuilder
{
    @Nonnull
    public static Filter create( @Nonnull Class<?> type, @Nullable String additionalFilter )
    {
        FilterBuilder filterBuilder = new FilterBuilder();
        filterBuilder.addClass( type );
        filterBuilder.add( additionalFilter );
        try
        {
            return FrameworkUtil.createFilter( filterBuilder.toString() );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalArgumentException( "could not compose filter of class '" + type.getName() + "' and additional filter '" + additionalFilter + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    private final List<String> filters = new LinkedList<>();

    public FilterBuilder addClass( @Nullable Class<?> clazz )
    {
        if( clazz != null )
        {
            addClass( clazz.getName() );
        }
        return this;
    }

    public FilterBuilder addClass( @Nullable String className )
    {
        if( className != null )
        {
            className = className.trim();
            if( !className.isEmpty() )
            {
                addEquals( Constants.OBJECTCLASS, className );
            }
        }
        return this;
    }

    public FilterBuilder addEquals( @Nullable String key, @Nullable String value )
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

    public FilterBuilder add( @Nullable String filter )
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

    @Override
    public String toString()
    {
        if( this.filters.isEmpty() )
        {
            return null;
        }
        else if( this.filters.size() == 1 )
        {
            return this.filters.get( 0 );
        }
        else
        {
            StringBuilder buf = new StringBuilder( 200 );
            for( String filter : this.filters )
            {
                buf.append( filter );
            }
            return "(&" + buf + ")";
        }
    }
}
