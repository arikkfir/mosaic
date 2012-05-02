package org.mosaic.util.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.ConversionService;

/**
 * @author arik
 */
public class WrappingTypedDict<V> extends WrappingDict<V> implements TypedDict<V>
{

    protected final ConversionService conversionService;

    private final Class<V> valueType;

    public WrappingTypedDict( ConversionService conversionService, Class<V> valueType )
    {
        this.conversionService = conversionService;
        this.valueType = valueType;
    }

    public WrappingTypedDict( Map<String, List<V>> map, ConversionService conversionService, Class<V> valueType )
    {
        super( map );
        this.conversionService = conversionService;
        this.valueType = valueType;
    }

    public WrappingTypedDict( Map<String, List<V>> map,
                              ListFactory<V> vListFactory,
                              ConversionService conversionService,
                              Class<V> valueType )
    {
        super( map, vListFactory );
        this.conversionService = conversionService;
        this.valueType = valueType;
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type )
    {
        V value = getValue( key );
        if( value != null )
        {
            return this.conversionService.convert( value, type );
        }
        else
        {
            return null;
        }
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type, T defaultValue )
    {
        V value = getValue( key );
        if( value != null )
        {
            return this.conversionService.convert( value, type );
        }
        else
        {
            return defaultValue;
        }
    }

    @Override
    public <T> T requireValueAs( String key, Class<T> type )
    {
        return this.conversionService.convert( requireValue( key ), type );
    }

    @Override
    public <T> void addAs( String key, T value )
    {
        add( key, this.conversionService.convert( value, this.valueType ) );
    }

    @Override
    public <T> void putAs( String key, T value )
    {
        put( key, this.conversionService.convert( value, this.valueType ) );
    }

    @Override
    public <T> Map<String, T> toMapAs( Class<T> type )
    {
        Map<String, T> map = new HashMap<>( size( ) );
        for( String key : keySet( ) )
        {
            map.put( key, getValueAs( key, type ) );
        }
        return map;
    }
}
