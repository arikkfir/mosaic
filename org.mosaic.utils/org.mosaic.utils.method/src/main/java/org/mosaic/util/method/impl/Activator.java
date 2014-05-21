package org.mosaic.util.method.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.util.method.MethodHandleFactory;
import org.osgi.framework.*;

/**
 * @author arik
 */
public class Activator implements BundleActivator, BundleListener
{
    @Nullable
    private ServiceRegistration<MethodHandleFactory> registration;

    private MethodHandleFactoryImpl methodHandleFactory;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        context.addBundleListener( this );
        this.methodHandleFactory = new MethodHandleFactoryImpl();
        this.registration = context.registerService( MethodHandleFactory.class, this.methodHandleFactory, null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<MethodHandleFactory> registration = this.registration;
        if( registration != null )
        {
            try
            {
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
            this.methodHandleFactory = null;
        }

        context.removeBundleListener( this );
    }

    @Override
    public void bundleChanged( @Nonnull BundleEvent event )
    {
        MethodHandleFactoryImpl methodHandleFactory = this.methodHandleFactory;
        if( methodHandleFactory != null && event.getType() == BundleEvent.UNRESOLVED )
        {
            methodHandleFactory.clearCaches();
        }
    }
}
