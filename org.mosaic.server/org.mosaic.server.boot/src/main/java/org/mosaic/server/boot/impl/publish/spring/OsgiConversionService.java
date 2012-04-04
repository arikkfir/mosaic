package org.mosaic.server.boot.impl.publish.spring;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * @author arik
 */
public class OsgiConversionService implements ConversionService {

    public static class ConversionServiceNotAvailableException extends ConversionException {

        public ConversionServiceNotAvailableException() {
            super( "Conversion service tracker is closed" );
        }
    }

    private final Bundle bundle;

    private ServiceTracker<Converter, Converter> convertersTracker;

    private DefaultFormattingConversionService conversionService;

    public OsgiConversionService( Bundle bundle ) {
        this.bundle = bundle;
    }

    public void open() {
        this.conversionService = new DefaultFormattingConversionService();
        this.convertersTracker = new ServiceTracker<>( this.bundle.getBundleContext(), Converter.class, new ServiceTrackerCustomizer<Converter, Converter>() {
            @Override
            public Converter addingService( ServiceReference<Converter> sr ) {
                return possiblyAddConverter( sr );
            }

            @Override
            public void modifiedService( ServiceReference<Converter> sr, Converter service ) {
                //no-op
            }

            @Override
            public void removedService( ServiceReference<Converter> sr, Converter service ) {
                removeMatchingConverters( sr );
            }
        } );
        this.convertersTracker.open();
    }

    public void close() {
        this.convertersTracker.close();
        this.convertersTracker = null;
        this.conversionService = null;
    }

    @Override
    public boolean canConvert( Class<?> sourceType, Class<?> targetType ) {
        DefaultFormattingConversionService conversionService = this.conversionService;
        return conversionService != null && conversionService.canConvert( sourceType, targetType );
    }

    @Override
    public boolean canConvert( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        DefaultFormattingConversionService conversionService = this.conversionService;
        return conversionService != null && conversionService.canConvert( sourceType, targetType );
    }

    @Override
    public <T> T convert( Object source, Class<T> targetType ) {
        DefaultFormattingConversionService conversionService = this.conversionService;
        if( conversionService != null ) {
            return conversionService.convert( source, targetType );
        } else {
            throw new ConversionServiceNotAvailableException();
        }
    }

    @Override
    public Object convert( Object source, TypeDescriptor sourceType, TypeDescriptor targetType ) {
        DefaultFormattingConversionService conversionService = this.conversionService;
        if( conversionService != null ) {
            return conversionService.convert( source, sourceType, targetType );
        } else {
            throw new ConversionServiceNotAvailableException();
        }
    }

    private Converter possiblyAddConverter( ServiceReference<Converter> sr ) {
        Object convertsFromValue = sr.getProperty( "convertsFrom" );
        if( !Class.class.isInstance( convertsFromValue ) ) {
            return null;
        }
        Class<?> fromClass = ( Class<?> ) convertsFromValue;

        Object convertsToValue = sr.getProperty( "convertsTo" );
        if( !Class.class.isInstance( convertsToValue ) ) {
            return null;
        }
        Class<?> toClass = ( Class<?> ) convertsToValue;

        Converter converter = this.bundle.getBundleContext().getService( sr );
        this.conversionService.addConverter( fromClass, toClass, converter );
        return converter;
    }

    private void removeMatchingConverters( ServiceReference<Converter> sr ) {
        Object convertsFromValue = sr.getProperty( "convertsFrom" );
        if( !Class.class.isInstance( convertsFromValue ) ) {
            return;
        }
        Class<?> fromClass = ( Class<?> ) convertsFromValue;

        Object convertsToValue = sr.getProperty( "convertsTo" );
        if( !Class.class.isInstance( convertsToValue ) ) {
            return;
        }
        Class<?> toClass = ( Class<?> ) convertsToValue;

        this.conversionService.removeConvertible( fromClass, toClass );
    }
}
