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
@SuppressWarnings("UnusedDeclaration")
public class UnmodifiableMapEx<K, V> implements MapEx<K, V>
{
    @Nonnull
    public static <K, V> MapEx<K, V> of( @Nonnull MapEx<K, V> target )
    {
        return new UnmodifiableMapEx<>( target );
    }

    @Nonnull
    private final MapEx<K, V> target;

    private UnmodifiableMapEx( @Nonnull MapEx<K, V> target )
    {
        this.target = target;
    }

    @Nonnull
    @Override
    public Optional<V> find( @Nonnull K key )
    {
        return this.target.find( key );
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull TypeToken<T> type )
    {
        return this.target.find( key, type );
    }

    @Nonnull
    @Override
    public <T> Optional<T> find( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.find( key, type );
    }

    @Override
    public int size()
    {
        return this.target.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.target.isEmpty();
    }

    @Override
    public boolean containsKey( Object key )
    {
        return this.target.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return this.target.containsValue( value );
    }

    @Override
    public V get( Object key )
    {
        return this.target.get( key );
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
        return Collections.unmodifiableSet( this.target.keySet() );
    }

    @Nonnull
    @Override
    public Collection<V> values()
    {
        return Collections.unmodifiableCollection( this.target.values() );
    }

    @Nonnull
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return Collections.unmodifiableSet( this.target.entrySet() );
    }
}
