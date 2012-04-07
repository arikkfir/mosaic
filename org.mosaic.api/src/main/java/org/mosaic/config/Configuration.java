package org.mosaic.config;

import java.util.Map;

/**
 * @author arik
 */
public interface Configuration extends Map<String, String> {

    <T> T getAs( String key, Class<T> type );

    <T> T getAs( String key, Class<T> type, T defaultValue );

}
