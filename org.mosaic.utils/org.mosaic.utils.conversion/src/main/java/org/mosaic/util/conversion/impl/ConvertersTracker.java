package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.conversion.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author arik
 */
final class ConvertersTracker
{
    @Nonnull
    private final ServiceTracker<Converter, Converter> serviceTracker;

    @Nonnull
    private final ConvertersGraph convertersGraph;

    ConvertersTracker( @Nonnull final BundleContext bundleContext, @Nonnull ConvertersGraph convertersGraph )
    {
        this.convertersGraph = convertersGraph;

        // the service tracker will track converter services and update our converters graph accordingly
        this.serviceTracker = new ServiceTracker<>( bundleContext, Converter.class, new ServiceTrackerCustomizer<Converter, Converter>()
        {
            @Override
            public Converter addingService( @Nonnull ServiceReference<Converter> reference )
            {
                Converter converter = bundleContext.getService( reference );
                if( converter != null )
                {
                    ConvertersTracker.this.convertersGraph.addConverter( converter );
                }
                return converter;
            }

            @Override
            public void modifiedService( @Nonnull ServiceReference<Converter> reference, @Nonnull Converter service )
            {
                // no-op
            }

            @Override
            public void removedService( @Nonnull ServiceReference<Converter> reference, @Nonnull Converter converter )
            {
                ConvertersTracker.this.convertersGraph.removeConverter( converter );
            }
        } );
    }

    void open()
    {
        this.serviceTracker.open();
    }

    void close()
    {
        this.serviceTracker.close();
    }

    @Nonnull
    ConvertersGraph getConvertersGraph()
    {
        return this.convertersGraph;
    }
}
