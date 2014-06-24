package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.ResolvedType;
import java.util.LinkedList;
import java.util.List;
import org.mosaic.core.components.MethodEndpoint;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceProvider;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.services.ServicesProvider;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class ServiceKey
{
    @Nonnull
    private static final Module.ServiceProperty[] EMPTY_SERVICE_PROPERTIES_ARRAY = new Module.ServiceProperty[ 0 ];

    @Nonnull
    private final ResolvedType resolvedServiceType;

    private final int minCount;

    @Nonnull
    private final List<Module.ServiceProperty> serviceProperties;

    ServiceKey( @Nonnull ResolvedType resolvedType, int minCount, @Nonnull Module.ServiceProperty... properties )
    {
        List<Module.ServiceProperty> propertiesList = new LinkedList<>();
        propertiesList.addAll( asList( properties ) );
        this.resolvedServiceType = discoverServiceType( resolvedType, propertiesList );
        this.minCount = minCount;
        this.serviceProperties = propertiesList;
    }

    @Nonnull
    public ResolvedType getServiceType()
    {
        return this.resolvedServiceType;
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

    @Override
    public int hashCode()
    {
        int result = this.resolvedServiceType.hashCode();
        result = 31 * result + this.serviceProperties.hashCode();
        result = 31 * result + this.minCount;
        return result;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.resolvedServiceType.getSignature() )
                             .add( "properties", this.serviceProperties )
                             .add( "min", this.minCount )
                             .toString();
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
        if( !this.resolvedServiceType.equals( that.resolvedServiceType ) )
        {
            return false;
        }
        if( this.minCount != that.minCount )
        {
            return false;
        }

        return true;
    }

    @Nonnull
    private ResolvedType validateNotOneOf( @Nonnull ResolvedType type, @Nonnull Class<?>... forbiddenTypes )
    {
        Class<?> erasedType = type.getErasedType();
        for( Class<?> forbiddenType : forbiddenTypes )
        {
            if( erasedType.equals( forbiddenType ) )
            {
                throw new IllegalStateException( "illegal type found: " + erasedType.getName() );
            }
        }
        return type;
    }

    @Nonnull
    private ResolvedType getTypeParameter( @Nonnull ResolvedType type, int index )
    {
        List<ResolvedType> serviceProviderParams = type.getTypeParameters();
        if( serviceProviderParams.isEmpty() )
        {
            throw new IllegalStateException( type + " has no type parameter" );
        }
        else
        {
            return serviceProviderParams.get( index );
        }
    }

    @Nonnull
    private ResolvedType discoverServiceType( @Nonnull ResolvedType resolvedType,
                                              @Nonnull List<Module.ServiceProperty> properties )
    {
        Class<?> erasedType = resolvedType.getErasedType();
        if( erasedType.equals( ServiceProvider.class ) || erasedType.equals( ServicesProvider.class ) || erasedType.equals( ServiceRegistration.class ) )
        {
            return discoverServiceType( validateNotOneOf( getTypeParameter( resolvedType, 0 ),
                                                          ServiceProvider.class,
                                                          ServicesProvider.class,
                                                          ServiceRegistration.class ),
                                        properties );
        }
        else if( erasedType.equals( List.class ) )
        {
            return discoverServiceType( validateNotOneOf( getTypeParameter( resolvedType, 0 ),
                                                          ServiceProvider.class,
                                                          ServicesProvider.class ),
                                        properties );
        }
        else if( erasedType.equals( MethodEndpoint.class ) )
        {
            properties.add( Module.ServiceProperty.p( "type", getTypeParameter( resolvedType, 0 ).getErasedType().getName() ) );
            return resolvedType;
        }
        else
        {
            // FEATURE: detect ClassEndpoint here, when it will be implement
            return resolvedType;
        }
    }
}
