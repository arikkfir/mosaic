package org.mosaic.util.collection;

import java.util.List;
import java.util.Map;

/**
 * @author arik
 */
public interface MultiMapAccessor<K, V> extends Map<K, List<V>>
{
    List<V> replace( K key, V value );

    V getFirst( K key );

    V getFirst( K key, V defaultValue );

    V requireFirst( K key );

    <T> T getFirst( K key, Class<T> type );

    <T> T requireFirst( K key, Class<T> type );

    <T> T getFirst( K key, Class<T> type, T defaultValue );

    void add( K key, V value );
}
