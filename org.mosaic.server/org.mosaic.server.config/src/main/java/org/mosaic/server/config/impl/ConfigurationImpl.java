package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import org.mosaic.config.Configuration;
import org.mosaic.util.collection.TypedDict;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.convert.ConversionService;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static org.mosaic.util.logging.LoggerFactory.getBundleLogger;

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

    private TypedDict<String> data;

    private long modificationTime;

    public ConfigurationImpl( BundleContext bundleContext, Path path, ConversionService conversionService ) {
        this.bundleContext = bundleContext;
        this.path = path;
        this.conversionService = conversionService;
        this.data = createEmptyMap();

        String fileName = this.path.getFileName().toString();
        this.name = fileName.substring( 0, fileName.indexOf( '.' ) );
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
                this.data = createEmptyMap();
                this.modificationTime = 0;
                unregister();

            }

        } else {

            try {

                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime ) {
                    this.modificationTime = modificationTime;

                    logger.info( "Refreshing configuration '{}' from: {}", this.name, this.path );
                    TypedDict<String> data = createEmptyMap();
                    try( InputStream inputStream = newInputStream( this.path, READ ) ) {

                        Properties properties = new Properties();
                        properties.load( inputStream );
                        for( String propertyName : properties.stringPropertyNames() ) {
                            data.put( propertyName, properties.getProperty( propertyName ) );
                        }

                    }

                    this.data = data;
                    register();
                }

            } catch( IOException e ) {
                logger.error( "Could not refresh configuration '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }

        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int size() {
        return this.data.size();
    }

    @Override
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    @Override
    public boolean containsKey( Object key ) {
        return this.data.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value ) {
        return this.data.containsValue( value );
    }

    @Override
    public List<String> get( Object key ) {
        return this.data.get( key );
    }

    @Override
    public List<String> put( String key, List<String> value ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public List<String> remove( Object key ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public void putAll( Map<? extends String, ? extends List<String>> m ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet( this.data.keySet() );
    }

    @Override
    public Collection<List<String>> values() {
        return Collections.unmodifiableCollection( this.data.values() );
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return Collections.unmodifiableSet( this.data.entrySet() );
    }

    @Override
    public String getValue( String key ) {
        return this.data.getValue( key );
    }

    @Override
    public String getValue( String key, String defaultValue ) {
        return this.data.getValue( key, defaultValue );
    }

    @Override
    public String requireValue( String key ) {
        return this.data.requireValue( key );
    }

    @Override
    public void add( String key, String value ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public void put( String key, String value ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public Map<String, String> toMap() {
        return Collections.unmodifiableMap( this.data.toMap() );
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type ) {
        return this.data.getValueAs( key, type );
    }

    @Override
    public <T> T getValueAs( String key, Class<T> type, T defaultValue ) {
        return this.data.getValueAs( key, type, defaultValue );
    }

    @Override
    public <T> T requireValueAs( String key, Class<T> type ) {
        return this.data.requireValueAs( key, type );
    }

    @Override
    public <T> void addAs( String key, T value ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public <T> void putAs( String key, T value ) {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public <T> Map<String, T> toMapAs( Class<T> type ) {
        return Collections.unmodifiableMap( this.data.toMapAs( type ) );
    }

    private WrappingTypedDict<String> createEmptyMap() {
        return createMap( new HashMap<String, List<String>>() );
    }

    private WrappingTypedDict<String> createMap( Map<String, List<String>> data ) {
        return new WrappingTypedDict<>( data, this.conversionService, String.class );
    }
}
