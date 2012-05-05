package org.mosaic.server.web.jetty.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collection.MapWrapper;
import org.mosaic.web.HttpSession;
import org.springframework.core.convert.ConversionService;

/**
 * @author arik
 */
public class HttpSessionImpl implements HttpSession
{
    private final javax.servlet.http.HttpSession session;

    private final MapWrapper<String, Object> attributes;

    public HttpSessionImpl( javax.servlet.http.HttpSession session, ConversionService conversionService )
    {
        this.session = session;
        this.attributes = new MapWrapper<>( new ConcurrentHashMap<String, Object>(), conversionService );
    }

    @Override
    public DateTime getCreationTime()
    {
        return new DateTime( this.session.getCreationTime() );
    }

    @Override
    public String getId()
    {
        return this.session.getId();
    }

    @Override
    public DateTime getLastAccessTime()
    {
        return new DateTime( this.session.getLastAccessedTime() );
    }

    @Override
    public Duration getMaxInactiveInterval()
    {
        return Duration.standardSeconds( this.session.getMaxInactiveInterval() );
    }

    @Override
    public boolean isNew()
    {
        return this.session.isNew();
    }

    @Override
    public void invalidate()
    {
        this.session.invalidate();
    }

    @Override
    public int size()
    {
        return attributes.size();
    }

    @Override
    public boolean isEmpty()
    {
        return attributes.isEmpty();
    }

    @Override
    public boolean containsKey( Object key )
    {
        return attributes.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return attributes.containsValue( value );
    }

    @Override
    public Object get( Object key )
    {
        return attributes.get( key );
    }

    @Override
    public Object put( String key, Object value )
    {
        return attributes.put( key, value );
    }

    @Override
    public Object remove( Object key )
    {
        return attributes.remove( key );
    }

    @Override
    public void putAll( Map<? extends String, ? extends Object> m )
    {
        attributes.putAll( m );
    }

    @Override
    public void clear()
    {
        attributes.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return attributes.keySet();
    }

    @Override
    public Collection<Object> values()
    {
        return attributes.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet()
    {
        return attributes.entrySet();
    }

    @Override
    public Object get( String key, Object defaultValue )
    {
        return attributes.get( key, defaultValue );
    }

    @Override
    public Object require( String key )
    {
        return attributes.require( key );
    }

    @Override
    public <T> T get( String key, Class<T> type )
    {
        return attributes.get( key, type );
    }

    @Override
    public <T> T require( String key, Class<T> type )
    {
        return attributes.require( key, type );
    }

    @Override
    public <T> T get( String key, Class<T> type, T defaultValue )
    {
        return attributes.get( key, type, defaultValue );
    }
}
