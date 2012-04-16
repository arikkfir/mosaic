package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import org.mosaic.config.Configuration;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.convert.ConversionService;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static org.mosaic.logging.LoggerFactory.getBundleLogger;

/**
 * @author arik
 */
public class ConfigurationImpl implements Configuration {

    private final BundleContext bundleContext;

    private final Logger logger;

    private final String name;

    private final Path path;

    private final ConversionService conversionService;

    private ServiceRegistration<Configuration> registration;

    private Map<String, String> data = Collections.emptyMap();

    private long modificationTime;

    public ConfigurationImpl( BundleContext bundleContext, Path path, ConversionService conversionService ) {
        this.bundleContext = bundleContext;
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

    public synchronized void register() {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put( "name", this.name );
        dict.put( "path", this.path.toString() );
        dict.put( "modificationTime", this.modificationTime );

        if( this.registration == null ) {
            this.registration = this.bundleContext.registerService( Configuration.class, this, dict );
        } else {
            this.registration.setProperties( dict );
        }
    }

    public synchronized void unregister() {
        if( this.registration != null ) {
            try {
                this.registration.unregister();
            } catch( IllegalStateException ignore ) {
            }
        }
    }

    public synchronized void refresh() {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) ) {

            if( this.modificationTime > 0 ) {

                logger.warn( "Configuration '{}' no longer exists/readable at: {}", this.name, this.path );
                this.data = Collections.emptyMap();
                this.modificationTime = 0;
                unregister();

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
                    register();
                }

            } catch( IOException e ) {
                logger.error( "Could not refresh configuration '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }

        }
    }

    public <T> T get( String key, Class<T> type ) {
        return get( key, type, null );
    }

    @Override
    public <T> T get( String key, Class<T> type, T defaultValue ) {
        String stringValue = get( key );
        if( stringValue == null ) {
            return defaultValue;
        } else {
            return this.conversionService.convert( stringValue, type );
        }
    }

    @Override
    public <T> T require( String key, Class<T> type ) {
        T value = get( key, type );
        if( value == null ) {
            throw new IllegalStateException( "Value is missing for key '" + key + "'" );
        } else {
            return value;
        }
    }

    @Override
    public <T> T require( String key, Class<T> type, T defaultValue ) {
        T value = get( key, type, defaultValue );
        if( value == null ) {
            throw new IllegalStateException( "Value is missing for key '" + key + "'" );
        } else {
            return value;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove( Object key ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll( Map<? extends String, ? extends String> m ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
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
