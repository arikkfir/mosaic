package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import static org.mosaic.util.collections.ConversionServiceTracker.conversionService;

/**
 * @author arik
 */
public class HierarchicalMap<K, V> implements MapEx<K, V>
{
    @Nonnull
    private final Deque<Pair<String, Map<K, V>>> stack = new LinkedList<>();

    public HierarchicalMap()
    {
        this( new HashMap<K, V>() );
    }

    public HierarchicalMap( @Nonnull Map<K, V> map )
    {
        this.stack.push( Pair.of( "root", map ) );
    }

    public HierarchicalMap<K, V> push( @Nonnull String name )
    {
        return push( name, new HashMap<K, V>() );
    }

    public HierarchicalMap<K, V> push( @Nonnull String name, @Nonnull Map<K, V> map )
    {
        this.stack.addFirst( Pair.of( name, map ) );
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

    @SuppressWarnings({ "SuspiciousMethodCalls" })
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
    public void putAll( @SuppressWarnings("NullableProblems") Map<? extends K, ? extends V> m )
    {
        this.stack.getFirst().getValue().putAll( m );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>NOTE: this method <b>only clears the entries for the bottom (ie. last) map in the hierarchy</b>!</p>
     */
    @Override
    public void clear()
    {
        this.stack.getFirst().getValue().clear();
    }

    @Nonnull
    @Override
    public Set<K> keySet()
    {
        return new HierarchicalKeySet();
    }

    @Nonnull
    @Override
    public Collection<V> values()
    {
        return new HierarchicalValueSet();
    }

    @Nonnull
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return new HierarchicalEntrySet();
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

    private class HierarchicalEntrySet extends AbstractSet<Map.Entry<K, V>>
    {
        @Override
        public int size()
        {
            return HierarchicalMap.this.size();
        }

        @Nonnull
        @Override
        public Iterator<Entry<K, V>> iterator()
        {
            return new HierarchicalEntryIterator();
        }

        @Override
        public boolean contains( Object o )
        {
            @SuppressWarnings("unchecked")
            Entry<K, V> entry = ( Entry<K, V> ) o;
            return HierarchicalMap.this.containsKey( entry.getKey() );
        }

        @Override
        public boolean removeAll( Collection<?> c )
        {
            boolean changed = false;
            for( Object o : c )
            {
                @SuppressWarnings("unchecked")
                Entry<K, V> entry = ( Entry<K, V> ) o;
                if( HierarchicalMap.this.containsKey( entry.getKey() ) )
                {
                    HierarchicalMap.this.remove( entry.getKey() );
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean remove( Object o )
        {
            @SuppressWarnings("unchecked")
            Entry<K, V> entry = ( Entry<K, V> ) o;
            if( HierarchicalMap.this.containsKey( entry.getKey() ) )
            {
                HierarchicalMap.this.remove( entry.getKey() );
                return true;
            }
            else
            {
                return false;
            }
        }

        @Override
        public void clear()
        {
            HierarchicalMap.this.clear();
        }

        private class HierarchicalEntryIterator implements Iterator<Entry<K, V>>
        {
            @Nonnull
            private final Iterator<Pair<String, Map<K, V>>> iterator = HierarchicalMap.this.stack.iterator();

            @Nullable
            private Iterator<Entry<K, V>> mapIterator;

            private int mapIndex = -1;

            @Nullable
            private Entry<K, V> lastEntry;

            private boolean removedCalled = false;

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext() || ( this.mapIterator != null && this.mapIterator.hasNext() );
            }

            @Override
            public Entry<K, V> next()
            {
                while( this.mapIterator == null || !this.mapIterator.hasNext() )
                {
                    this.mapIterator = this.iterator.next().getValue().entrySet().iterator();
                    this.mapIndex++;
                }

                this.lastEntry = this.mapIterator.next();
                this.removedCalled = false;
                return this.lastEntry;
            }

            @Override
            public void remove()
            {
                if( this.removedCalled )
                {
                    throw new IllegalStateException( "remove() method already called" );
                }
                else if( this.mapIterator == null || this.mapIndex < 0 || this.lastEntry == null )
                {
                    throw new IllegalStateException( "next() method has not been called" );
                }
                else if( this.mapIndex == 0 )
                {
                    this.mapIterator.remove();
                    this.removedCalled = true;
                }
                else
                {
                    throw new UnsupportedOperationException( "only elements from last pushed map can be removed" );
                }
            }
        }
    }

    private class HierarchicalKeySet extends AbstractSet<K>
    {
        @Override
        public int size()
        {
            return HierarchicalMap.this.size();
        }

        @Nonnull
        @Override
        public Iterator<K> iterator()
        {
            return new HierarchicalKeyIterator();
        }

        @Override
        public boolean contains( Object o )
        {
            return HierarchicalMap.this.containsKey( o );
        }

        @Override
        public boolean removeAll( Collection<?> c )
        {
            boolean changed = false;
            for( Object o : c )
            {
                if( HierarchicalMap.this.containsKey( o ) )
                {
                    HierarchicalMap.this.remove( o );
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean remove( Object o )
        {
            if( HierarchicalMap.this.containsKey( o ) )
            {
                HierarchicalMap.this.remove( o );
                return true;
            }
            else
            {
                return false;
            }
        }

        @Override
        public void clear()
        {
            HierarchicalMap.this.clear();
        }

        private class HierarchicalKeyIterator implements Iterator<K>
        {
            @Nonnull
            private final Iterator<Pair<String, Map<K, V>>> iterator = HierarchicalMap.this.stack.iterator();

            @Nullable
            private Iterator<K> keyIterator;

            private int mapIndex = -1;

            @Nullable
            private K lastKey;

            private boolean removedCalled = false;

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext() || ( this.keyIterator != null && this.keyIterator.hasNext() );
            }

            @Override
            public K next()
            {
                while( this.keyIterator == null || !this.keyIterator.hasNext() )
                {
                    this.keyIterator = this.iterator.next().getValue().keySet().iterator();
                    this.mapIndex++;
                }

                this.lastKey = this.keyIterator.next();
                this.removedCalled = false;
                return this.lastKey;
            }

            @Override
            public void remove()
            {
                if( this.removedCalled )
                {
                    throw new IllegalStateException( "remove() method already called" );
                }
                else if( this.keyIterator == null || this.mapIndex < 0 || this.lastKey == null )
                {
                    throw new IllegalStateException( "next() method has not been called" );
                }
                else if( this.mapIndex == 0 )
                {
                    this.keyIterator.remove();
                    this.removedCalled = true;
                }
                else
                {
                    throw new UnsupportedOperationException( "only elements from last pushed map can be removed" );
                }
            }
        }
    }

    private class HierarchicalValueSet extends AbstractSet<V>
    {
        @Override
        public int size()
        {
            return HierarchicalMap.this.size();
        }

        @Nonnull
        @Override
        public Iterator<V> iterator()
        {
            return new HierarchicalValueIterator();
        }

        @Override
        public boolean contains( Object o )
        {
            return HierarchicalMap.this.containsValue( o );
        }

        @Override
        public boolean removeAll( Collection<?> c )
        {
            return HierarchicalMap.this.stack.getFirst().getValue().values().removeAll( c );
        }

        @Override
        public boolean remove( Object o )
        {
            return HierarchicalMap.this.stack.getFirst().getValue().values().remove( o );
        }

        @Override
        public void clear()
        {
            HierarchicalMap.this.clear();
        }

        private class HierarchicalValueIterator implements Iterator<V>
        {
            @Nonnull
            private final Iterator<Pair<String, Map<K, V>>> iterator = HierarchicalMap.this.stack.iterator();

            @Nullable
            private Iterator<Entry<K, V>> mapIterator;

            private int mapIndex = -1;

            @Nullable
            private Entry<K, V> lastEntry;

            private boolean removedCalled = false;

            @Override
            public boolean hasNext()
            {
                return this.iterator.hasNext() || ( this.mapIterator != null && this.mapIterator.hasNext() );
            }

            @Override
            public V next()
            {
                while( this.mapIterator == null || !this.mapIterator.hasNext() )
                {
                    this.mapIterator = this.iterator.next().getValue().entrySet().iterator();
                    this.mapIndex++;
                }

                this.lastEntry = this.mapIterator.next();
                this.removedCalled = false;
                return this.lastEntry.getValue();
            }

            @Override
            public void remove()
            {
                if( this.removedCalled )
                {
                    throw new IllegalStateException( "remove() method already called" );
                }
                else if( this.mapIterator == null || this.mapIndex < 0 || this.lastEntry == null )
                {
                    throw new IllegalStateException( "next() method has not been called" );
                }
                else if( this.mapIndex == 0 )
                {
                    this.mapIterator.remove();
                    this.removedCalled = true;
                }
                else
                {
                    throw new UnsupportedOperationException( "only elements from last pushed map can be removed" );
                }
            }
        }
    }
}
