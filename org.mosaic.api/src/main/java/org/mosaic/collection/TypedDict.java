package org.mosaic.collection;

import java.util.Map;

/**
 * @author arik
 */
public interface TypedDict<V> extends Dict<V> {

    <T> T getValueAs( String key, Class<T> type );

    <T> T getValueAs( String key, Class<T> type, T defaultValue );

    <T> T requireValueAs( String key, Class<T> type );

    <T> void addAs( String key, T value );

    <T> void putAs( String key, T value );

    <T> Map<String, T> toMapAs( Class<T> type );

}
