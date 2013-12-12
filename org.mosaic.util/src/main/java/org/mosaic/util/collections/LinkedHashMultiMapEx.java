package org.mosaic.util.collections;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.conversion.impl.ConversionActivator;

import static java.lang.String.format;

/**
 * @author arik
 */
public class LinkedHashMultiMapEx<K, V> extends LinkedHashMap<K, List<V>> implements MultiMapEx<K, V>
{
    @Nonnull
    private final ConversionService conversionService = ConversionActivator.getConversionService();

    public LinkedHashMultiMapEx( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }

    public LinkedHashMultiMapEx( int initialCapacity )
    {
        super( initialCapacity );
    }

    public LinkedHashMultiMapEx()
    {
    }

    public LinkedHashMultiMapEx( @Nonnull Map<? extends K, ? extends List<V>> m )
    {
        super( m );
    }

    public LinkedHashMultiMapEx( int initialCapacity, float loadFactor, boolean accessOrder )
    {
        super( initialCapacity, loadFactor, accessOrder );
    }

    @Override
    public V getOne( @Nonnull K key )
    {
        List<V> values = get( key );
        if( values == null || values.isEmpty() )
        {
            return null;
        }
        else
        {
            return values.get( 0 );
        }
    }

    @Override
    public V getOne( @Nonnull K key, @Nullable V defaultValue )
    {
        V value = getOne( key );
        return value == null ? defaultValue : value;
    }

    @Nonnull
    @Override
    public V requireOne( @Nonnull K key )
    {
        V value = getOne( key );
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
    public <T> T getOne( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.conversionService.convert( getOne( key ), type );
    }

    @Nonnull
    @Override
    public <T> T requireOne( @Nonnull K key, @Nonnull Class<T> type )
    {
        T value = getOne( key, type );
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
    public <T> T getOne( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue )
    {
        T value = getOne( key, type );
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
    public <T> List<T> getAll( @Nonnull K key, @Nonnull final Class<T> type )
    {
        List<V> values = get( key );
        return Lists.transform( values, new Function<V, T>()
        {
            @Nullable
            @Override
            public T apply( @Nullable V input )
            {
                return conversionService.convert( input, type );
            }
        } );
    }

    @Override
    public void addOne( @Nonnull K key, @Nullable V value )
    {
        List<V> values = get( key );
        if( values == null )
        {
            values = new LinkedList<>();
            put( key, values );
        }
        values.add( value );
    }
}
