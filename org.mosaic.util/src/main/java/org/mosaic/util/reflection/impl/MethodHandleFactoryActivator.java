package org.mosaic.util.reflection.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandleFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public final class MethodHandleFactoryActivator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<MethodHandleFactory> methodHandleFactoryServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext bundleContext ) throws Exception
    {
        this.methodHandleFactoryServiceRegistration = bundleContext.registerService( MethodHandleFactory.class, new MethodHandleFactoryImpl(), null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.methodHandleFactoryServiceRegistration != null )
        {
            try
            {
                this.methodHandleFactoryServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
        }
        this.methodHandleFactoryServiceRegistration = null;
    }
}
