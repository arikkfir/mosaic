package org.mosaic.util.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class UnmodifiableMapEx<K, V> implements MapEx<K, V>
{
    @Nonnull
    private final MapEx<K, V> target;

    public UnmodifiableMapEx( @Nonnull MapEx<K, V> target )
    {
        this.target = target;
    }

    @Override
    public V get( @Nonnull K key, @Nullable V defaultValue )
    {
        return this.target.get( key, defaultValue );
    }

    @Nonnull
    @Override
    public V require( @Nonnull K key )
    {
        return this.target.require( key );
    }

    @Nullable
    @Override
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.get( key, type );
    }

    @Nonnull
    @Override
    public <T> T require( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.require( key, type );
    }

    @Override
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue )
    {
        return this.target.get( key, type, defaultValue );
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
    public Set<Entry<K, V>> entrySet()
    {
        return Collections.unmodifiableSet( this.target.entrySet() );
    }
}
