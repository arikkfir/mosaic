package org.mosaic.lifecycle.impl.registrar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.osgi.framework.Constants;

/**
 * @author arik
 */
public class BeanServiceRegistrar extends AbstractRegistrar
{
    @Nonnull
    private final Class<?> serviceType;

    @Nonnull
    private final DP[] properties;

    public BeanServiceRegistrar( @Nonnull ModuleImpl module,
                                 @Nonnull String beanName,
                                 @Nonnull Class<?> serviceType,
                                 int rank,
                                 @Nonnull Service.P... properties )
    {
        super( module, beanName );
        this.serviceType = serviceType;

        this.properties = new DP[ properties.length + 1 ];
        this.properties[ 0 ] = DP.dp( Constants.SERVICE_RANKING, rank );
        for( int i = 0; i < properties.length; i++ )
        {
            Service.P property = properties[ i ];
            this.properties[ i + 1 ] = DP.dp( property.key(), property.value() );
        }
    }

    @Nonnull
    @Override
    protected Class<?> getServiceType()
    {
        return this.serviceType;
    }

    @Nullable
    @Override
    protected Object getServiceInstance()
    {
        return this.module.getBean( this.beanName );
    }

    @Nonnull
    @Override
    protected DP[] getServiceProperties()
    {
        return this.properties;
    }
}
