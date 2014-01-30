package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class EmptyMapEx<K, V> implements MapEx<K, V>
{
    @Nonnull
    public static <K, V> MapEx<K, V> emptyMapEx()
    {
        return new EmptyMapEx<>();
    }

    private EmptyMapEx()
    {
    }

    @Nonnull
    @Override
    public Optional<V> find( @Nonnull K key )
    {
        return Optional.absent();
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        return Optional.absent();
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull Class<T> type )
    {
        return Optional.absent();
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean containsKey( Object key )
    {
        return false;
    }

    @Override
    public boolean containsValue( Object value )
    {
        return false;
    }

    @Override
    public V get( Object key )
    {
        return null;
    }

    @Override
    public V put( K key, V value )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove( Object key )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll( @Nonnull Map<? extends K, ? extends V> m )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Set<K> keySet()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<V> values()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return Collections.emptySet();
    }
}
