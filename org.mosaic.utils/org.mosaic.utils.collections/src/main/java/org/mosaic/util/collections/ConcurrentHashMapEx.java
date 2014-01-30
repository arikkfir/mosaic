package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import static org.mosaic.util.collections.ConversionServiceTracker.conversionService;

/**
 * @author arik
 */
public class ConcurrentHashMapEx<K, V> extends ConcurrentHashMap<K, V> implements MapEx<K, V>
{
    public ConcurrentHashMapEx( int initialCapacity, float loadFactor, int concurrencyLevel )
    {
        super( initialCapacity, loadFactor, concurrencyLevel );
    }

    public ConcurrentHashMapEx( int initialCapacity, float loadFactor )
    {
        super( initialCapacity, loadFactor );
    }

    public ConcurrentHashMapEx( int initialCapacity )
    {
        super( initialCapacity );
    }

    public ConcurrentHashMapEx()
    {
        super();
    }

    public ConcurrentHashMapEx( Map<? extends K, ? extends V> m )
    {
        super( m );
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
