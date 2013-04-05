package org.mosaic.util.collect;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.mosaic.util.convert.ConversionService;

import static java.lang.String.format;

/**
 * @author arik
 */
public class HashMapEx<K, V> extends HashMap<K, V> implements MapEx<K, V>
{
    @Nonnull
    private final ConversionService conversionService;

    public HashMapEx( int initialCapacity,
                      float loadFactor,
                      @Nonnull ConversionService conversionService )
    {
        super( initialCapacity, loadFactor );
        this.conversionService = conversionService;
    }

    public HashMapEx( int initialCapacity, @Nonnull ConversionService conversionService )
    {
        super( initialCapacity );
        this.conversionService = conversionService;
    }

    public HashMapEx( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    public HashMapEx( @Nonnull Map<? extends K, ? extends V> m, @Nonnull ConversionService conversionService )
    {
        super( m );
        this.conversionService = conversionService;
    }

    @Override
    public V get( K key, V defaultValue )
    {
        V value = get( key );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return defaultValue;
        }
    }

    @Override
    public V require( K key )
    {
        V value = get( key );
        if( value == null )
        {
            throw new RequiredKeyMissingException( format( "no value for key '%s'", key ), key );
        }
        else
        {
            return value;
        }
    }

    @Override
    public <T> T get( K key, Class<T> type )
    {
        V value = get( key );
        if( value == null )
        {
            return null;
        }
        else if( type.isInstance( value ) )
        {
            return type.cast( value );
        }
        else
        {
            return this.conversionService.convert( value, type );
        }
    }

    @Override
    public <T> T require( K key, Class<T> type )
    {
        T value = get( key, type );
        if( value == null )
        {
            throw new RequiredKeyMissingException( format( "no value for key '%s'", key ), key );
        }
        else
        {
            return value;
        }
    }

    @Override
    public <T> T get( K key, Class<T> type, T defaultValue )
    {
        T value = get( key, type );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }
}
