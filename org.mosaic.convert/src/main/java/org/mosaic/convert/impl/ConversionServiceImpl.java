package org.mosaic.convert.impl;

import org.mosaic.convert.ConversionService;
import org.mosaic.convert.Converter;
import org.mosaic.core.components.Component;
import org.mosaic.core.components.OnServiceRegistration;
import org.mosaic.core.components.OnServiceUnregistration;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
@Component(ConversionService.class)
final class ConversionServiceImpl implements ConversionService
{
    @Nonnull
    private final ConvertersGraph convertersGraph;

    ConversionServiceImpl()
    {
        this.convertersGraph = new ConvertersGraph();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <Source, Dest> Dest convert( @Nullable Source source, @Nonnull Class<Dest> targetType )
    {
        // null always returns null
        if( source == null )
        {
            return null;
        }

        // avoid conversion if not necessary
        Class<?> sourceType = source.getClass();
        if( targetType.isAssignableFrom( sourceType ) )
        {
            return ( Dest ) source;
        }

        // find converter and convert; if no converter found, the 'getConverter' method will throw a ConversionException
        Converter converter = this.convertersGraph.getConverter( sourceType, targetType );
        return ( Dest ) converter.convert( source );
    }

    @OnServiceRegistration
    void converterAdded( @Nonnull ServiceRegistration<Converter<?, ?>> registration )
    {
        Converter<?, ?> converter = registration.getService();
        if( converter != null )
        {
            this.convertersGraph.addConverter( converter );
        }
    }

    @OnServiceUnregistration
    void converterRemoved( @Nonnull ServiceRegistration<Converter<?, ?>> registration,
                           @Nonnull Converter<?, ?> converter )
    {
        this.convertersGraph.removeConverter( converter );
    }
}
