package org.mosaic.util.collection;

import java.util.Map;

/**
 * @author arik
 */
public interface MapAccessor<K, V> extends Map<K, V>
{
    V get( String key, V defaultValue );

    V require( String key );

    <T> T get( String key, Class<T> type );

    <T> T require( String key, Class<T> type );

    <T> T get( String key, Class<T> type, T defaultValue );
}
