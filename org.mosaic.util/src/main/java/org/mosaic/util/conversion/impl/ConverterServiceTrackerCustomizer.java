package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.conversion.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author arik
 */
final class ConverterServiceTrackerCustomizer implements ServiceTrackerCustomizer<Converter, Converter>
{
    @Nonnull
    private final ConversionServiceImpl conversionService;

    private final BundleContext bundleContext;

    ConverterServiceTrackerCustomizer( @Nonnull BundleContext bundleContext,
                                       @Nonnull ConversionServiceImpl conversionService )
    {
        this.bundleContext = bundleContext;
        this.conversionService = conversionService;
    }

    @Override
    public synchronized Converter addingService( @Nonnull ServiceReference<Converter> reference )
    {
        Converter<?, ?> service = this.bundleContext.getService( reference );
        if( service != null )
        {
            this.conversionService.registerConverter( service );
            return service;
        }
        return null;
    }

    @Override
    public void modifiedService( @Nonnull ServiceReference<Converter> reference,
                                 @Nonnull Converter service )
    {
        // no-op
    }

    @Override
    public synchronized void removedService( @Nonnull ServiceReference<Converter> reference,
                                             @Nonnull Converter service )
    {
        this.conversionService.unregisterConverter( service );
    }
}
