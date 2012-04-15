package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static org.mosaic.logging.LoggerFactory.getBundleLogger;

/**
 * @author arik
 */
public class ConfigurationImpl implements Configuration {

    private final Logger logger;

    private final String name;

    private final Path path;

    private final ConversionService conversionService;

    private Map<String, String> data = Collections.emptyMap();

    private long modificationTime;

    public ConfigurationImpl( Path path, ConversionService conversionService ) {
        this.path = path;
        String fileName = this.path.getFileName().toString();
        this.name = fileName.substring( 0, fileName.indexOf( '.' ) );
        this.conversionService = conversionService;
        this.logger = LoggerFactory.getLogger(
                getBundleLogger( ConfigurationManager.class ).getName() + "." + this.name );
    }

    public Path getPath() {
        return path;
    }

    public boolean matches( String pattern ) {
        return this.path.getFileSystem().getPathMatcher( pattern ).matches( this.path );
    }

    public void invoke( MethodEndpointInfo listener ) {
        try {
            logger.debug( "Invoking @ConfigListener '{}' for configuration '{}'", listener, this.name );
            listener.invoke( this );
        } catch( Exception e ) {
            logger.error( "Error invoking @ConfigListener for configuration '{}': {}", this.name, e.getMessage(), e );
        }
    }

    public synchronized boolean refresh() {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) ) {

            if( this.modificationTime > 0 ) {

                logger.warn( "Configuration '{}' no longer exists/readable at: {}", this.name, this.path );
                this.data = Collections.emptyMap();
                this.modificationTime = 0;
                return true;

            }

        } else {

            try {

                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime ) {
                    this.modificationTime = modificationTime;

                    logger.info( "Refreshing configuration '{}' from: {}", this.name, this.path );
                    Map<String, String> data = new HashMap<>();
                    try( InputStream inputStream = newInputStream( this.path, READ ) ) {

                        Properties properties = new Properties();
                        properties.load( inputStream );
                        for( String propertyName : properties.stringPropertyNames() ) {
                            data.put( propertyName, properties.getProperty( propertyName ) );
                        }

                    }

                    this.data = Collections.unmodifiableMap( data );
                    return true;
                }

            } catch( IOException e ) {
                logger.error( "Could not refresh configuration '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }

        }
        return false;
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

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {
        return data.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value ) {
        return data.containsValue( value );
    }

    @Override
    public String get( Object key ) {
        return data.get( key );
    }

    @Override
    public String put( String key, String value ) {
        return data.put( key, value );
    }

    @Override
    public String remove( Object key ) {
        return data.remove( key );
    }

    @Override
    public void putAll( Map<? extends String, ? extends String> m ) {
        data.putAll( m );
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<String> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return data.entrySet();
    }
}
