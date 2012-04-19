package org.mosaic.collection;

import java.util.List;
import java.util.Map;

/**
 * @author arik
 */
public interface Dict<V> extends Map<String, List<V>> {

    interface ListFactory<V> {

        List<V> createList();

    }

    V getValue( String key );

    V getValue( String key, V defaultValue );

    V requireValue( String key );

    void add( String key, V value );

    Map<String, V> toMap();

}
