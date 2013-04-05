package org.mosaic.lifecycle.impl.registrar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.ServicePropertiesProvider;
import org.mosaic.lifecycle.impl.ModuleImpl;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public abstract class AbstractRegistrar
{
    @Nonnull
    protected final ModuleImpl module;

    @Nonnull
    protected final String beanName;

    @Nullable
    protected ServiceRegistration<?> serviceRegistration;

    protected AbstractRegistrar( @Nonnull ModuleImpl module, @Nonnull String beanName )
    {
        this.module = module;
        this.beanName = beanName;
    }

    @SuppressWarnings("unchecked")
    public void register()
    {
        BundleContext bundleContext = this.module.getBundle().getBundleContext();
        if( bundleContext != null )
        {
            Object bean = getServiceInstance();
            if( bean != null )
            {
                Class serviceType = getServiceType();
                DP[] properties = getServiceProperties();

                if( bean instanceof ServicePropertiesProvider )
                {
                    ServicePropertiesProvider servicePropertiesProvider = ( ServicePropertiesProvider ) bean;
                    DP[] additionalProperties = servicePropertiesProvider.getServiceProperties();

                    DP[] finalProperties = new DP[ properties.length + additionalProperties.length ];
                    System.arraycopy( properties, 0, finalProperties, 0, properties.length );
                    System.arraycopy( additionalProperties, 0, finalProperties, properties.length, additionalProperties.length );
                    properties = finalProperties;
                }

                this.serviceRegistration = ServiceUtils.register( bundleContext, serviceType, serviceType.cast( bean ), properties );
            }
        }
    }

    public void unregister()
    {
        this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
    }

    @Nullable
    protected abstract Object getServiceInstance();

    @Nonnull
    protected abstract Class getServiceType();

    @Nonnull
    protected abstract DP[] getServiceProperties();
}
