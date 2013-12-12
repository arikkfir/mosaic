package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.conversion.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author arik
 */
public final class ConversionActivator implements BundleActivator
{
    @Nullable
    private static ConversionServiceImpl conversionService;

    @Nonnull
    public static ConversionServiceImpl getConversionService()
    {
        ConversionServiceImpl service = ConversionActivator.conversionService;
        if( service == null )
        {
            throw new IllegalStateException( "Conversion service not available" );
        }
        else
        {
            return service;
        }
    }

    @Nullable
    private ServiceTracker<Converter, Converter> convertersTracker;

    @Nullable
    private ServiceRegistration<ConversionService> conversionServiceServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext bundleContext ) throws Exception
    {
        ConversionServiceImpl conversionService = new ConversionServiceImpl();
        ConversionActivator.conversionService = conversionService;

        this.conversionServiceServiceRegistration = bundleContext.registerService( ConversionService.class, conversionService, null );

        this.convertersTracker = new ServiceTracker<>( bundleContext, Converter.class, new ConverterServiceTrackerCustomizer( bundleContext, conversionService ) );
        this.convertersTracker.open();
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.conversionServiceServiceRegistration != null )
        {
            try
            {
                this.conversionServiceServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
        }
        this.conversionServiceServiceRegistration = null;

        if( this.convertersTracker != null )
        {
            try
            {
                this.convertersTracker.close();
            }
            catch( Exception ignore )
            {
            }
        }
        this.convertersTracker = null;

        ConversionServiceImpl conversionService = ConversionActivator.conversionService;
        if( conversionService != null )
        {
            conversionService.close();
        }
        ConversionActivator.conversionService = null;
    }
}
