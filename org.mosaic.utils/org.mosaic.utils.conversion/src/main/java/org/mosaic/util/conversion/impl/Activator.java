package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public final class Activator implements BundleActivator
{
    @Nullable
    private ConvertersTracker convertersTracker;

    @Nullable
    private ServiceRegistration<ConversionService> registration;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        this.convertersTracker = new ConvertersTracker( context, new ConvertersGraph() );
        this.convertersTracker.open();

        ConversionServiceImpl conversionService = new ConversionServiceImpl( this.convertersTracker.getConvertersGraph() );
        this.registration = context.registerService( ConversionService.class, conversionService, null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<ConversionService> registration = this.registration;
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
        }

        ConvertersTracker convertersTracker = this.convertersTracker;
        if( convertersTracker != null )
        {
            try
            {
                convertersTracker.close();
            }
            catch( Exception ignore )
            {
            }
            this.convertersTracker = null;
        }
    }
}
