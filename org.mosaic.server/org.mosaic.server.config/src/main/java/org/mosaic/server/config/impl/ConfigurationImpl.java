package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;

/**
 * @author arik
 */
public class ConfigurationImpl implements Configuration {

    private static final Logger LOG = LoggerFactory.getBundleLogger( ConfigurationImpl.class );

    private final String name;

    private final Path path;

    private final ConversionService conversionService;

    private final Set<MethodEndpointInfo> listeners = new CopyOnWriteArraySet<>();

    private Map<String, String> data = Collections.emptyMap();

    private long modificationTime;

    public ConfigurationImpl( Path path, ConversionService conversionService ) {
        this.path = path;
        String fileName = this.path.getFileName().toString();
        this.name = fileName.substring( 0, fileName.indexOf( '.' ) );
        this.conversionService = conversionService;
    }

    public Path getPath() {
        return path;
    }

    public synchronized void addListener( MethodEndpointInfo listener ) {
        this.listeners.add( listener );
        refresh();
        invokeListener( listener );
    }

    public synchronized Collection<MethodEndpointInfo> getListeners() {
        return this.listeners;
    }

    public synchronized void removeListener( MethodEndpointInfo listener ) {
        this.listeners.remove( listener );
    }

    public synchronized void refresh() {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) ) {

            if( this.modificationTime > 0 ) {

                LOG.warn( "Configuration '{}' no longer exists/readable at: {}", this.name, this.path );
                this.data = Collections.emptyMap();
                this.modificationTime = 0;
                invokeListeners();

            }

        } else {

            try {
                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime ) {
                    this.modificationTime = modificationTime;

                    LOG.info( "Refreshing configuration '{}' from: {}", this.name, this.path );
                    Map<String, String> data = new HashMap<>();
                    try( InputStream inputStream = newInputStream( this.path, READ ) ) {

                        Properties properties = new Properties();
                        properties.load( inputStream );
                        for( String propertyName : properties.stringPropertyNames() ) {
                            data.put( propertyName, properties.getProperty( propertyName ) );
                        }

                    }

                    this.data = data;
                    invokeListeners();
                }
            } catch( IOException e ) {
                LOG.error( "Could not refresh configuration '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }

        }
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

    private void invokeListeners() {
        for( MethodEndpointInfo listener : this.listeners ) {
            invokeListener( listener );
        }
    }

    private void invokeListener( MethodEndpointInfo listener ) {
        try {
            LOG.debug( "Invoking @ConfigListener '{}' for configuration '{}'", listener, this.name );
            listener.invoke( this );
        } catch( Exception e ) {
            LOG.error( "Error invoking @ConfigListener for configuration '{}': {}",
                       this.path.getFileName().toString(), e.getMessage(), e );
        }
    }
}
