package org.mosaic.core.impl.module;

import java.util.Collections;
import java.util.List;
import org.mosaic.core.Module;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class ServiceKey<ServiceType>
{

    private static final Module.ServiceProperty[] EMPTY_SERVICE_PROPERTIES_ARRAY = new Module.ServiceProperty[ 0 ];

    @Nonnull
    private final Class<ServiceType> serviceType;

    private final int minCount;

    @Nonnull
    private final List<Module.ServiceProperty> serviceProperties;

    ServiceKey( @Nonnull Class<ServiceType> serviceType, int minCount, @Nonnull Module.ServiceProperty... properties )
    {
        this.serviceType = serviceType;
        this.minCount = minCount;
        this.serviceProperties = properties.length == 0 ? Collections.<Module.ServiceProperty>emptyList() : asList( properties );
    }

    @Nonnull
    public Class<ServiceType> getServiceType()
    {
        return this.serviceType;
    }

    @Nonnull
    public List<Module.ServiceProperty> getServiceProperties()
    {
        return this.serviceProperties;
    }

    @Nonnull
    public Module.ServiceProperty[] getServicePropertiesArray()
    {
        if( this.serviceProperties.isEmpty() )
        {
            return EMPTY_SERVICE_PROPERTIES_ARRAY;
        }
        else
        {
            return this.serviceProperties.toArray( new Module.ServiceProperty[ this.serviceProperties.size() ] );
        }
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

        if( !this.serviceProperties.equals( that.serviceProperties ) )
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
        result = 31 * result + this.serviceProperties.hashCode();
        result = 31 * result + this.minCount;
        return result;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.serviceType.getSimpleName() )
                             .add( "properties", this.serviceProperties )
                             .add( "min", this.minCount )
                             .toString();
    }
}
