package org.mosaic.util.collect;

import java.util.Map;

/**
 * @author arik
 */
public interface MapEx<K, V> extends Map<K, V>
{
    V get( K key, V defaultValue );

    V require( K key );

    <T> T get( K key, Class<T> type );

    <T> T require( K key, Class<T> type );

    <T> T get( K key, Class<T> type, T defaultValue );
}
