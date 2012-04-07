package org.mosaic.server.config.impl;

import java.util.HashMap;
import org.mosaic.config.Configuration;
import org.springframework.core.convert.ConversionService;

/**
 * @author arik
 */
public class ConfigurationImpl extends HashMap<String, String> implements Configuration {

    private final ConversionService conversionService;

    public ConfigurationImpl( ConversionService conversionService ) {
        this.conversionService = conversionService;
    }

    public <T> T getAs( String key, Class<T> type ) {
        return getAs( key, type, null );
    }

    @Override
    public <T> T getAs( String key, Class<T> type, T defaultValue ) {
        String stringValue = get( key );
        if( stringValue == null ) {
            return defaultValue;
        } else {
            return this.conversionService.convert( stringValue, type );
        }
    }
}
