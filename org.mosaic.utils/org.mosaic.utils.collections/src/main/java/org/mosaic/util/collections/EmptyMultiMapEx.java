package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Collections.emptyList;

/**
 * @author arik
 */
public class EmptyMultiMapEx<K, V> implements MultiMapEx<K, V>
{
    @Nonnull
    public static <K, V> MultiMapEx<K, V> emptyMultiMapEx()
    {
        return new EmptyMultiMapEx<>();
    }

    private EmptyMultiMapEx()
    {
    }

    @Nonnull
    @Override
    public Optional<V> findFirst( @Nonnull K key )
    {
        return Optional.absent();
    }

    @Nonnull
    @Override
    public <T> Optional<T> findFirst( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        return Optional.absent();
    }

    @Nonnull
    @Override
    public <T> Optional<T> findFirst( @Nonnull K key, @Nonnull Class<T> type )
    {
        return Optional.absent();
    }

    @Nonnull
    @Override
    public <T> List<T> find( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        return emptyList();
    }

    @Nonnull
    @Override
    public <T> List<T> find( @Nonnull K key, @Nonnull Class<T> type )
    {
        return emptyList();
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
    public List<V> get( Object key )
    {
        return null;
    }

    @Override
    public List<V> put( K key, List<V> value )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> remove( Object key )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll( @Nonnull Map<? extends K, ? extends List<V>> m )
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
    public Collection<List<V>> values()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Set<Entry<K, List<V>>> entrySet()
    {
        return Collections.emptySet();
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
