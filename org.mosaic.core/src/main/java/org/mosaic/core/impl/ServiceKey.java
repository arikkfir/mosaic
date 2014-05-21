package org.mosaic.core.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.osgi.framework.Filter;

/**
 * @author arik
 */
final class ServiceKey<ServiceType>
{
    @Nonnull
    private final Class<ServiceType> serviceType;

    @Nullable
    private final Filter filter;

    private final int minCount;

    ServiceKey( @Nonnull Class<ServiceType> serviceType, @Nullable Filter filter, int minCount )
    {
        this.serviceType = serviceType;
        this.filter = filter;
        this.minCount = minCount;
    }

    @Nonnull
    public Class<ServiceType> getServiceType()
    {
        return this.serviceType;
    }

    @Nullable
    public Filter getFilter()
    {
        return this.filter;
    }

    public int getMinCount()
    {
        return this.minCount;
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

        ServiceKey that = ( ServiceKey ) o;

        if( this.filter != null ? !this.filter.equals( that.filter ) : that.filter != null )
        {
            return false;
        }
        if( !this.serviceType.equals( that.serviceType ) )
        {
            return false;
        }
        if( this.minCount != that.minCount )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = this.serviceType.hashCode();
        result = 31 * result + ( this.filter != null ? this.filter.hashCode() : 0 );
        result = 31 * result + this.minCount;
        return result;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.serviceType.getSimpleName() )
                             .add( "filter", this.filter )
                             .add( "min", this.minCount )
                             .toString();
    }
}
