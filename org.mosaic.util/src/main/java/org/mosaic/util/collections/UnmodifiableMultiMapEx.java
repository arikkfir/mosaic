package org.mosaic.util.collections;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
@SuppressWarnings( "UnusedDeclaration" )
public class UnmodifiableMultiMapEx<K, V> implements MultiMapEx<K, V>
{
    @Nonnull
    public static <K, V> MultiMapEx<K, V> of( @Nonnull MultiMapEx<K, V> target )
    {
        return new UnmodifiableMultiMapEx<>( target );
    }

    @Nonnull
    private final MultiMapEx<K, V> target;

    private UnmodifiableMultiMapEx( @Nonnull MultiMapEx<K, V> target )
    {
        this.target = target;
    }

    @Override
    public V getOne( @Nonnull K key )
    {
        return this.target.getOne( key );
    }

    @Override
    public V getOne( @Nonnull K key, @Nullable V defaultValue )
    {
        return this.target.getOne( key, defaultValue );
    }

    @Nonnull
    @Override
    public V requireOne( @Nonnull K key )
    {
        return this.target.requireOne( key );
    }

    @Nullable
    @Override
    public <T> T getOne( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.getOne( key, type );
    }

    @Nonnull
    @Override
    public <T> T requireOne( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.requireOne( key, type );
    }

    @Override
    public <T> T getOne( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue )
    {
        return this.target.getOne( key, type, defaultValue );
    }

    @Nonnull
    @Override
    public <T> List<T> getAll( @Nonnull K key, @Nonnull Class<T> type )
    {
        return this.target.getAll( key, type );
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
    public List<V> get( Object key )
    {
        return this.target.get( key );
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
        return Collections.unmodifiableSet( this.target.keySet() );
    }

    @Nonnull
    @Override
    public Collection<List<V>> values()
    {
        return Collections.unmodifiableCollection( this.target.values() );
    }

    @Nonnull
    @Override
    public Set<Entry<K, List<V>>> entrySet()
    {
        return Collections.unmodifiableSet( this.target.entrySet() );
    }

    @Override
    public void addOne( @Nonnull K key, @Nullable V value )
    {
        throw new UnsupportedOperationException();
    }
}
