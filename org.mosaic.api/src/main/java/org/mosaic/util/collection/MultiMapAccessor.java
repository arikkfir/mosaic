package org.mosaic.util.collection;

import java.util.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import static java.lang.String.format;

/**
 * @author arik
 */
public class MultiMapAccessor<K, V> implements Map<K, List<V>>
{
    private final Map<K, List<V>> map;

    private final ConversionService conversionService;

    public MultiMapAccessor( Map<K, List<V>> map )
    {
        this( map, new DefaultFormattingConversionService( true ) );
    }

    public MultiMapAccessor( Map<K, List<V>> map, ConversionService conversionService )
    {
        this.map = map;
        this.conversionService = conversionService;
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey( Object key )
    {
        return this.map.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        if( this.map.containsValue( value ) )
        {
            return true;
        }
        else
        {
            for( List<?> list : this.map.values() )
            {
                if( list.contains( value ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public List<V> get( Object key )
    {
        return this.map.get( key );
    }

    @Override
    public List<V> put( K key, List<V> value )
    {
        return this.map.put( key, value );
    }

    public List<V> replace( K key, V value )
    {
        return this.map.put( key, Arrays.asList( value ) );
    }

    @Override
    public List<V> remove( Object key )
    {
        return this.map.remove( key );
    }

    @Override
    public void putAll( Map<? extends K, ? extends List<V>> m )
    {
        this.map.putAll( m );
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return this.map.keySet();
    }

    @Override
    public Collection<List<V>> values()
    {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet()
    {
        return this.map.entrySet();
    }

    public V getFirst( K key )
    {
        List<V> list = get( key );
        if( list == null || list.isEmpty() )
        {
            return null;
        }
        else
        {
            return list.get( 0 );
        }
    }

    public V getFirst( K key, V defaultValue )
    {
        V value = getFirst( key );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return defaultValue;
        }
    }

    public V requireFirst( K key )
    {
        V value = getFirst( key );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "no value for key '%s'", key ) );
        }
        else
        {
            return value;
        }
    }

    public <T> T getFirst( K key, Class<T> type )
    {
        V value = getFirst( key );
        if( value == null )
        {
            return null;
        }
        else
        {
            return this.conversionService.convert( value, type );
        }
    }

    public <T> T requireFirst( K key, Class<T> type )
    {
        T value = getFirst( key, type );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "no value for key '%s'", key ) );
        }
        else
        {
            return value;
        }
    }

    public <T> T getFirst( K key, Class<T> type, T defaultValue )
    {
        T value = getFirst( key, type );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    public void add( K key, V value )
    {
        List<V> list = this.map.get( key );
        if( list == null )
        {
            list = new ArrayList<>( 10 );
            this.map.put( key, list );
        }
        list.add( value );
    }
}
