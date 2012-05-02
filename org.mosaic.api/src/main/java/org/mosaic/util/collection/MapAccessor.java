package org.mosaic.util.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import static java.lang.String.format;

/**
 * @author arik
 */
public class MapAccessor<K, V> implements Map<K, V>
{
    private final Map<K, V> map;

    private final ConversionService conversionService;

    public MapAccessor( Map<K, V> map )
    {
        this( map, new DefaultFormattingConversionService( true ) );
    }

    public MapAccessor( Map<K, V> map, ConversionService conversionService )
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
        return this.map.containsValue( value );
    }

    @Override
    public V get( Object key )
    {
        return this.map.get( key );
    }

    @Override
    public V put( K key, V value )
    {
        return this.map.put( key, value );
    }

    @Override
    public V remove( Object key )
    {
        return this.map.remove( key );
    }

    @Override
    public void putAll( Map<? extends K, ? extends V> m )
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
    public Collection<V> values()
    {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return this.map.entrySet();
    }

    public V get( String key, V defaultValue )
    {
        V value = get( key );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return defaultValue;
        }
    }

    public V require( String key )
    {
        V value = get( key );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "no value for key '%s'", key ) );
        }
        else
        {
            return value;
        }
    }

    public <T> T get( String key, Class<T> type )
    {
        V value = get( key );
        if( value == null )
        {
            return null;
        }
        else
        {
            return this.conversionService.convert( value, type );
        }
    }

    public <T> T require( String key, Class<T> type )
    {
        T value = get( key, type );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "no value for key '%s'", key ) );
        }
        else
        {
            return value;
        }
    }

    public <T> T get( String key, Class<T> type, T defaultValue )
    {
        T value = get( key, type );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }
}
