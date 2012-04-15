package org.mosaic.config;

import java.util.Map;

/**
 * @author arik
 */
public interface Configuration extends Map<String, String> {

    String getName();

    <T> T get( String key, Class<T> type );

    <T> T get( String key, Class<T> type, T defaultValue );

    <T> T require( String key, Class<T> type );

    <T> T require( String key, Class<T> type, T defaultValue );

}
