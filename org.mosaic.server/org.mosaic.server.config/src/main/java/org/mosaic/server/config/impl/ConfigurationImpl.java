package org.mosaic.server.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import org.mosaic.config.Configuration;
import org.mosaic.util.logging.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.convert.ConversionService;

import static java.lang.String.format;
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

    private final ConversionService conversionService;

    private ServiceRegistration<Configuration> registration;

    private Map<String, String> values = Collections.emptyMap( );

    private long modificationTime;

    public ConfigurationImpl( BundleContext bundleContext, Path path, ConversionService conversionService )
    {
        this.bundleContext = bundleContext;
        this.path = path;
        this.conversionService = conversionService;

        String fileName = this.path.getFileName( ).toString( );
        this.name = fileName.substring( 0, fileName.indexOf( '.' ) );
        this.logger = getBundleLogger( ConfigurationManager.class, this.name );
    }

    @Override
    public String getName( )
    {
        return this.name;
    }

    public Path getPath( )
    {
        return path;
    }

    public synchronized void register( )
    {
        Dictionary<String, Object> dict = new Hashtable<>( );
        dict.put( "name", this.name );
        dict.put( "path", this.path.toString( ) );
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

    public synchronized void unregister( )
    {
        if( this.registration != null )
        {
            try
            {
                this.registration.unregister( );
            }
            catch( IllegalStateException ignore )
            {
            }
        }
    }

    public synchronized void refresh( )
    {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) )
        {
            if( this.modificationTime > 0 )
            {
                logger.warn( "Configuration '{}' no longer exists/readable at: {}", this.name, this.path );
                this.values = Collections.emptyMap( );
                this.modificationTime = 0;
                unregister( );
            }
        }
        else
        {
            try
            {
                long modificationTime = getLastModifiedTime( this.path ).toMillis( );
                if( modificationTime > this.modificationTime )
                {
                    this.modificationTime = modificationTime;

                    logger.info( "Refreshing configuration '{}' from: {}", this.name, this.path );
                    Map<String, String> values = new HashMap<>( );
                    try( InputStream inputStream = newInputStream( this.path, READ ) )
                    {
                        Properties properties = new Properties( );
                        properties.load( inputStream );
                        for( String propertyName : properties.stringPropertyNames( ) )
                        {
                            values.put( propertyName, properties.getProperty( propertyName ) );
                        }
                    }
                    this.values = values;
                    register( );
                }
            }
            catch( IOException e )
            {
                logger.error( "Could not refresh configuration '{}': {}", this.path.getFileName( ).toString( ), e.getMessage( ), e );
            }
        }
    }

    @Override
    public int size( )
    {
        return this.values.size( );
    }

    @Override
    public boolean isEmpty( )
    {
        return this.values.isEmpty( );
    }

    @Override
    public boolean containsKey( Object key )
    {
        return this.values.containsKey( key );
    }

    @Override
    public boolean containsValue( Object value )
    {
        return this.values.containsValue( value );
    }

    @Override
    public String get( Object key )
    {
        return this.values.get( key );
    }

    @Override
    public String put( String key, String value )
    {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public String remove( Object key )
    {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public void putAll( Map<? extends String, ? extends String> m )
    {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public void clear( )
    {
        throw new UnsupportedOperationException( "Configurations cannot be modified" );
    }

    @Override
    public Set<String> keySet( )
    {
        return Collections.unmodifiableSet( this.values.keySet( ) );
    }

    @Override
    public Collection<String> values( )
    {
        return Collections.unmodifiableCollection( this.values.values( ) );
    }

    @Override
    public Set<Entry<String, String>> entrySet( )
    {
        return Collections.unmodifiableSet( this.values.entrySet( ) );
    }

    @Override
    public String get( String key, String defaultValue )
    {
        String value = this.values.get( key );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return defaultValue;
        }
    }

    @Override
    public String require( String key )
    {
        String value = get( key );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "Configuration '%s' has no value for key '%s'", this.name, key ) );
        }
        else
        {
            return value;
        }
    }

    @Override
    public <T> T get( String key, Class<T> type )
    {
        String value = get( key );
        if( value == null )
        {
            return null;
        }
        else
        {
            return this.conversionService.convert( value, type );
        }
    }

    @Override
    public <T> T require( String key, Class<T> type )
    {
        T value = get( key, type );
        if( value == null )
        {
            throw new IllegalArgumentException( format( "Configuration '%s' has no value for key '%s'", this.name, key ) );
        }
        else
        {
            return value;
        }
    }

    @Override
    public <T> T get( String key, Class<T> type, T defaultValue )
    {
        T value = get( key, type );
        if( value == null )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }
}
