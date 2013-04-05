package org.mosaic.lifecycle.impl.util;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import static org.osgi.framework.Constants.OBJECTCLASS;

/**
 * @author arik
 */
public abstract class FilterUtils
{
    public static Filter createFilter( String filter )
    {
        try
        {
            return FrameworkUtil.createFilter( filter );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }
    }

    public static Filter createFilter( Class<?> serviceType, String additionalFilter )
    {
        String classFilter = "(" + OBJECTCLASS + "=" + serviceType.getName() + ")";
        String filterString;
        if( additionalFilter != null && additionalFilter.trim().length() > 0 )
        {
            if( !additionalFilter.startsWith( "(" ) )
            {
                additionalFilter = "(" + additionalFilter + ")";
            }
            filterString = "(&" + classFilter + additionalFilter + ")";
        }
        else
        {
            filterString = classFilter;
        }

        Filter filter;
        try
        {
            filter = FrameworkUtil.createFilter( filterString );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalArgumentException( "Illegal filter: " + filterString, e );
        }
        return filter;
    }
}
