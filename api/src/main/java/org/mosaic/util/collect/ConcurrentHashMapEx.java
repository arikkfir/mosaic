package org.mosaic.util.collect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.convert.ConversionService;

import static java.lang.String.format;

/**
 * @author arik
 */
public class ConcurrentHashMapEx<K, V> extends ConcurrentHashMap<K, V> implements MapEx<K, V>
{
    @Nonnull
    protected final ConversionService conversionService;

    public ConcurrentHashMapEx( int initialCapacity,
                                float loadFactor,
                                int concurrencyLevel,
                                @Nonnull ConversionService conversionService )
    {
        super( initialCapacity, loadFactor, concurrencyLevel );
        this.conversionService = conversionService;
    }

    public ConcurrentHashMapEx( int initialCapacity,
                                float loadFactor,
                                @Nonnull ConversionService conversionService )
    {
        super( initialCapacity, loadFactor );
        this.conversionService = conversionService;
    }

    public ConcurrentHashMapEx( int initialCapacity, @Nonnull ConversionService conversionService )
    {
        super( initialCapacity );
        this.conversionService = conversionService;
    }

    public ConcurrentHashMapEx( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    public ConcurrentHashMapEx( Map<? extends K, ? extends V> m,
                                @Nonnull ConversionService conversionService )
    {
        super( m );
        this.conversionService = conversionService;
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
            return defaultValue;
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