package org.mosaic.util.collections;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mosaic.util.collections.ConversionServiceTracker.conversionService;

/**
 * @author arik
 */
public class HashMultiMapEx<K, V> extends HashMap<K, List<V>> implements MultiMapEx<K, V>
{
    public HashMultiMapEx( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }

    public HashMultiMapEx( int initialCapacity )
    {
        super( initialCapacity );
    }

    public HashMultiMapEx()
    {
    }

    public HashMultiMapEx( @Nonnull Map<? extends K, ? extends List<V>> m )
    {
        super( m );
    }

    @Nonnull
    @Override
    public Optional<V> findFirst( @Nonnull K key )
    {
        List<V> values = get( key );
        return values == null || values.isEmpty() ? Optional.<V>absent() : Optional.fromNullable( values.get( 0 ) );
    }

    @Nonnull
    @Override
    public <T> Optional<T> findFirst( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        List<V> values = get( key );
        return values == null || values.isEmpty() ? Optional.<T>absent() : Optional.fromNullable( conversionService().convert( values.get( 0 ), type ) );
    }

    @Nonnull
    @Override
    public <T> Optional<T> findFirst( @Nonnull K key, @Nonnull Class<T> type )
    {
        List<V> values = get( key );
        return values == null || values.isEmpty() ? Optional.<T>absent() : Optional.fromNullable( conversionService().convert( values.get( 0 ), type ) );
    }

    @Nonnull
    @Override
    public <T> List<T> find( @Nonnull K key, @Nonnull final TypeToken<T> type )
    {
        List<V> values = get( key );
        return Lists.transform( values, new Function<V, T>()
        {
            @Nullable
            @Override
            public T apply( @Nullable V input )
            {
                return conversionService().convert( input, type );
            }
        } );
    }

    @Nonnull
    @Override
    public <T> List<T> find( @Nonnull K key, @Nonnull final Class<T> type )
    {
        List<V> values = get( key );
        return Lists.transform( values, new Function<V, T>()
        {
            @Nullable
            @Override
            public T apply( @Nullable V input )
            {
                return conversionService().convert( input, type );
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
