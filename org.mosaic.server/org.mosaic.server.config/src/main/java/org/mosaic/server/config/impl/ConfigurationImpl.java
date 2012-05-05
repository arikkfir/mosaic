package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import org.mosaic.config.Configuration;
import org.mosaic.util.collection.MapWrapper;
import org.mosaic.util.logging.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.convert.ConversionService;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static org.mosaic.util.logging.LoggerFactory.getBundleLogger;

/**
 * @author arik
 */
public class ConfigurationImpl implements Configuration
{
    private final BundleContext bundleContext;

    private final Logger logger;

    private final String name;

    private final Path path;

    private ServiceRegistration<Configuration> registration;

    private MapWrapper<String, String> values;

    private long modificationTime;

    public ConfigurationImpl( BundleContext bundleContext, Path path, ConversionService conversionService )
    {
        this.bundleContext = bundleContext;
        this.path = path;

        String fileName = this.path.getFileName().toString();
        this.name = fileName.substring( 0, fileName.indexOf( '.' ) );
        this.logger = getBundleLogger( ConfigurationManager.class, this.name );
        this.values = new MapWrapper<>( Collections.<String, String>emptyMap(), conversionService );
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    public Path getPath()
    {
        return path;
    }

    public synchronized void register()
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put( "name", this.name );
        dict.put( "path", this.path.toString() );
        dict.put( "modificationTime", this.modificationTime );
        if( this.registration == null )
        {
            this.registration = this.bundleContext.registerService( Configuration.class, this, dict );
        }
        else
        {
            this.registration.setProperties( dict );
        }
    }

    public synchronized void unregister()
    {
        if( this.registration != null )
        {
            try
            {
                this.registration.unregister();
            }
            catch( IllegalStateException ignore )
            {
            }
        }
    }

    public synchronized void refresh()
    {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) )
        {
            if( this.modificationTime > 0 )
            {
                logger.warn( "Configuration '{}' no longer exists/readable at: {}", this.name, this.path );
                this.values.setMap( Collections.<String, String>emptyMap() );
                this.modificationTime = 0;
                unregister();
            }
        }
        else
        {
            try
            {
                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime )
                {
                    this.modificationTime = modificationTime;

                    logger.info( "Refreshing configuration '{}' from: {}", this.name, this.path );
                    Map<String, String> values = new HashMap<>();
                    try( InputStream inputStream = newInputStream( this.path, READ ) )
                    {
                        Properties properties = new Properties();
                        properties.load( inputStream );
                        for( String propertyName : properties.stringPropertyNames() )
                        {
                            values.put( propertyName, properties.getProperty( propertyName ) );
                        }
                    }
                    this.values.setMap( Collections.unmodifiableMap( values ) );
                    register();
                }
            }
            catch( IOException e )
            {
                logger.error( "Could not refresh configuration '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }
        }
    }

    @Override
    public <T> T get( String key, Class<T> type, T defaultValue )
    {
        return values.get( key, type, defaultValue );
    }

    @Override
    public <T> T require( String key, Class<T> type )
    {
        return values.require( key, type );
    }

    @Override
    public <T> T get( String key, Class<T> type )
    {
        return values.get( key, type );
    }

    @Override
    public String require( String key )
    {
        return values.require( key );
    }

    @Override
    public String get( String key, String defaultValue )
    {
        return values.get( key, defaultValue );
    }

    @Override
    public Set<Entry<String, String>> entrySet()
    {
        return values.entrySet();
    }

    @Override
    public Collection<String> values()
    {
        return values.values();
    }

    @Override
    public Set<String> keySet()
    {
        return values.keySet();
    }

    @Override
    public void clear()
    {
        values.clear();
    }

    @Override
    public void putAll( Map<? extends String, ? extends String> m )
    {
        values.putAll( m );
    }

    @Override
    public String remove( Object key )
    {
        return values.remove( key );
    }

    @Override
    public String put( String key, String value )
    {
        return values.put( key, value );
    }

    @Override
    public String get( Object key )
    {
        return values.get( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return values.containsValue( value );
    }

    @Override
    public boolean containsKey( Object key )
    {
        return values.containsKey( key );
    }

    @Override
    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    @Override
    public int size()
    {
        return values.size();
    }
}
