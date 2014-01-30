package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import static org.mosaic.util.collections.ConversionServiceTracker.conversionService;

/**
 * @author arik
 */
public class LinkedHashMapEx<K, V> extends LinkedHashMap<K, V> implements MapEx<K, V>
{
    public LinkedHashMapEx( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }

    public LinkedHashMapEx( int initialCapacity )
    {
        super( initialCapacity );
    }

    public LinkedHashMapEx()
    {
    }

    public LinkedHashMapEx( @Nonnull Map<? extends K, ? extends V> m )
    {
        super( m );
    }

    public LinkedHashMapEx( int initialCapacity, float loadFactor, boolean accessOrder )
    {
        super( initialCapacity, loadFactor, accessOrder );
    }

    @Nonnull
    @Override
    public Optional<V> find( @Nonnull K key )
    {
        return Optional.fromNullable( get( key ) );
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        Optional<V> value = find( key );
        return value.isPresent() ? Optional.fromNullable( conversionService().convert( value.get(), type ) ) : Optional.<T>absent();
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull Class<T> type )
    {
        Optional<V> value = find( key );
        return value.isPresent() ? Optional.fromNullable( conversionService().convert( value.get(), type ) ) : Optional.<T>absent();
    }
}
