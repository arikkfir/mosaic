package org.mosaic.config;

import java.util.Map;

/**
 * @author arik
 */
public interface Configuration extends Map<String, String>
{
    String getName( );

    String get( String key, String defaultValue );

    String require( String key );

    <T> T get( String key, Class<T> type );

    <T> T require( String key, Class<T> type );

    <T> T get( String key, Class<T> type, T defaultValue );

}
