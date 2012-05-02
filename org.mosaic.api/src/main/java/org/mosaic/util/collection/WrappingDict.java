package org.mosaic.util.collection;

import java.util.*;

/**
 * @author arik
 */
public class WrappingDict<V> implements Dict<V>
{

    private static class ArrayListFactory<V> implements ListFactory<V>
    {

        private final int initialCapacity;

        private ArrayListFactory( )
        {
            this( 25 );
        }

        private ArrayListFactory( int initialCapacity )
        {
            this.initialCapacity = initialCapacity;
        }

        @Override
        public List<V> createList( )
        {
            return new ArrayList<>( this.initialCapacity );
        }
    }

    private final ListFactory<V> listFactory;

    private final Map<String, List<V>> map;

    public WrappingDict( )
    {
        this( new HashMap<String, List<V>>( ), new ArrayListFactory<V>( ) );
    }

    public WrappingDict( Map<String, List<V>> map )
    {
        this( map, new ArrayListFactory<V>( ) );
    }

    public WrappingDict( Map<String, List<V>> map, ListFactory<V> listFactory )
    {
        this.listFactory = listFactory;
        this.map = map;
    }

    public Map<String, List<V>> getMap( )
    {
        return map;
    }

    @Override
    public V getValue( String key )
    {
        List<V> values = this.map.get( key );
        return values == null || values.isEmpty( ) ? null : values.get( 0 );
    }

    @Override
    public V getValue( String key, V defaultValue )
    {
        List<V> values = this.map.get( key );
        return values == null || values.isEmpty( ) ? defaultValue : values.get( 0 );
    }

    @Override
    public V requireValue( String key )
    {
        V value = getValue( key );
        if( value != null )
        {
            return value;
        }
        else
        {
            throw new MissingRequiredValueException( key );
        }
    }

    @Override
    public void add( String key, V value )
    {
        List<V> values = this.map.get( key );
        if( values == null )
        {
            values = this.listFactory.createList( );
            this.map.put( key, values );
        }
        values.add( value );
    }

    @Override
    public void put( String key, V value )
    {
        List<V> values = this.map.get( key );
        if( values == null )
        {
            values = this.listFactory.createList( );
            this.map.put( key, values );
        }
        else
        {
            values.clear( );
        }
        values.add( value );
    }

    @Override
    public Map<String, V> toMap( )
    {
        Map<String, V> map = new HashMap<>( this.map.size( ) );
        for( String key : this.map.keySet( ) )
        {
            map.put( key, getValue( key ) );
        }
        return map;
    }

    @Override
    public int size( )
    {
        return map.size( );
    }

    @Override
    public boolean isEmpty( )
    {
        return map.isEmpty( );
    }

    @Override
    public boolean containsKey( Object key )
    {
        return map.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return map.containsValue( value );
    }

    @Override
    public List<V> get( Object key )
    {
        return map.get( key );
    }

    public List<V> put( String key, List<V> value )
    {
        return map.put( key, value );
    }

    @Override
    public List<V> remove( Object key )
    {
        return map.remove( key );
    }

    public void putAll( Map<? extends String, ? extends List<V>> m )
    {
        map.putAll( m );
    }

    @Override
    public void clear( )
    {
        map.clear( );
    }

    @Override
    public Set<String> keySet( )
    {
        return map.keySet( );
    }

    @Override
    public Collection<List<V>> values( )
    {
        return map.values( );
    }

    @Override
    public Set<Entry<String, List<V>>> entrySet( )
    {
        return map.entrySet( );
    }

    @Override
    public boolean equals( Object o )
    {
        if( o instanceof Map )
        {
            Map<?, ?> that = ( Map<?, ?> ) o;
            return this.map.equals( that );
        }
        else
        {
            return false;
        }
    }

    @Override
    public String toString( )
    {
        return this.map.toString( );
    }
}
