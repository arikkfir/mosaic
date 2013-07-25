package org.mosaic.web.handler.impl.adapter.page;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.collect.RequiredKeyMissingException;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.ImmutablePair;
import org.mosaic.util.pair.Pair;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;

/**
 * @author arik
 */
public class HierarchicalMap<K, V> implements MapEx<K, V>
{
    @Nonnull
    private final ConversionService conversionService;

    @Nonnull
    private final Deque<Pair<String, Map<K, V>>> stack = new LinkedList<>();

    public HierarchicalMap( @Nonnull ConversionService conversionService )
    {
        this( conversionService, new HashMap<K, V>() );
    }

    public HierarchicalMap( @Nonnull ConversionService conversionService, @Nonnull Map<K, V> map )
    {
        this.conversionService = conversionService;
        this.stack.push( ImmutablePair.of( "root", map ) );
    }

    public HierarchicalMap<K, V> push( @Nonnull String name )
    {
        return push( name, new HashMap<K, V>() );
    }

    public HierarchicalMap<K, V> push( @Nonnull String name, @Nonnull Map<K, V> map )
    {
        this.stack.addFirst( ImmutablePair.of( name, map ) );
        return this;
    }

    public HierarchicalMap<K, V> pop()
    {
        if( this.stack.size() == 1 )
        {
            throw new IllegalStateException( "Cannot remove root map" );
        }
        else
        {
            this.stack.removeFirst();
            return this;
        }
    }

    @Override
    public int size()
    {
        int size = 0;
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            size += entry.getValue().size();
        }
        return size;
    }

    @Override
    public boolean isEmpty()
    {
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            if( !entry.getValue().isEmpty() )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsKey( Object key )
    {
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            if( entry.getValue().containsKey( key ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue( Object value )
    {
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            if( entry.getValue().containsValue( value ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get( Object key )
    {
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            //noinspection SuspiciousMethodCalls
            if( entry.getValue().containsKey( key ) )
            {
                return entry.getValue().get( key );
            }
        }
        return null;
    }

    @Override
    public V put( K key, V value )
    {
        V oldValue = get( key );
        this.stack.getFirst().getValue().put( key, value );
        return oldValue;
    }

    @SuppressWarnings( { "SuspiciousMethodCalls" } )
    @Override
    public V remove( Object key )
    {
        if( this.stack.getFirst().getValue().containsKey( key ) )
        {
            return this.stack.getFirst().getValue().remove( key );
        }
        else if( containsKey( key ) )
        {
            throw new UnsupportedOperationException( "cannot remove key '" + key + "' from non-leaf map" );
        }
        else
        {
            return null;
        }
    }

    @Override
    public void putAll( @SuppressWarnings( "NullableProblems" ) Map<? extends K, ? extends V> m )
    {
        this.stack.getFirst().getValue().putAll( m );
    }

    @Override
    public void clear()
    {
        // TODO arik: document collections framework exception: we're not clearing parent keys here
        this.stack.getFirst().getValue().clear();
    }

    @Nonnull
    @Override
    public Set<K> keySet()
    {
        // TODO arik: document collections framework exception: keySet is not a view
        Set<K> keys = new HashSet<>();
        for( Pair<String, Map<K, V>> entry : this.stack )
        {
            keys.addAll( entry.getValue().keySet() );
        }
        return unmodifiableSet( keys );
    }

    @Nonnull
    @Override
    public Collection<V> values()
    {
        // TODO arik: document collections framework exception: values collection is not a view
        Collection<V> values = new LinkedList<>();
        for( K key : keySet() )
        {
            values.add( get( key ) );
        }
        return unmodifiableCollection( values );
    }

    @Nonnull
    @Override
    public Set<Entry<K, V>> entrySet()
    {
        // TODO arik: document collections framework exception: entrySet collection is not a view
        Set<Entry<K, V>> values = new HashSet<>();
        for( K key : keySet() )
        {
            V value = get( key );
            values.add( new AbstractMap.SimpleImmutableEntry<K, V>( key, value ) );
        }
        return unmodifiableSet( values );
    }

    @Override
    public V get( @Nonnull K key, @Nullable V defaultValue )
    {
        V value = get( key );
        return value != null ? value : defaultValue;
    }

    @Nonnull
    @Override
    public V require( @Nonnull K key )
    {
        V value = get( key );
        if( value != null )
        {
            return value;
        }
        else
        {
            throw new RequiredKeyMissingException( key );
        }
    }

    @Nullable
    @Override
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type )
    {
        V value = get( key );
        return value != null ? this.conversionService.convert( value, type ) : null;
    }

    @Nonnull
    @Override
    public <T> T require( @Nonnull K key, @Nonnull Class<T> type )
    {
        T value = get( key, type );
        if( value != null )
        {
            return value;
        }
        else
        {
            throw new RequiredKeyMissingException( key );
        }
    }

    @Override
    public <T> T get( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue )
    {
        T value = get( key, type );
        return value == null ? defaultValue : value;
    }
}
