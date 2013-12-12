package org.mosaic.util.collections;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.conversion.impl.ConversionActivator;

import static java.lang.String.format;

/**
 * @author arik
 */
public class HashMapEx<K, V> extends HashMap<K, V> implements MapEx<K, V>
{
    @Nonnull
    private final ConversionService conversionService = ConversionActivator.getConversionService();

    public HashMapEx( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }

    public HashMapEx( int initialCapacity )
    {
        super( initialCapacity );
    }

    public HashMapEx()
    {
    }

    public HashMapEx( @Nonnull Map<? extends K, ? extends V> m )
    {
        super( m );
    }

    @Override
    public V get( @Nonnull K key, @Nullable V defaultValue )
    {
        V value = get( key );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    @Nonnull
    @Override
    public V require( @Nonnull K key )
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
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.conversionService.convert( get( key ), type );
    }

    @Nonnull
    @Override
    public <T> T require( @Nonnull K key, @Nonnull Class<T> type )
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
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue )
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
