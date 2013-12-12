package org.mosaic.datasource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.pathwatchers.PathWatcher;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
@Component
final class DataSourceManager
{
    private static final Logger LOG = LoggerFactory.getLogger( DataSourceManager.class );

    private static final String DATASOURCE_FILES = "${mosaic.home.etc}/datasources/*.properties";

    @Nonnull
    private final Map<String, ConfigurableDataSource> dataSources = new ConcurrentHashMap<>();

    @Nonnull
    @Component
    private TransactionManagerImpl transactionManager;

    @Nonnull
    @Component
    private Module module;

    @PathWatcher(value = DATASOURCE_FILES, events = { CREATED, MODIFIED })
    synchronized void addDataSource( @Nonnull Path file )
    {
        MapEx<String, String> cfg = new HashMapEx<>( 20 );
        try( InputStream inputStream = Files.newInputStream( file ) )
        {
            Properties props = new Properties();
            props.load( inputStream );
            for( String propertyName : props.stringPropertyNames() )
            {
                cfg.put( propertyName, props.getProperty( propertyName ) );
            }
        }
        catch( IOException e )
        {
            LOG.warn( "Could not read data source from '{}': {}", file, e.getMessage(), e );
        }

        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.length() - ".properties".length() );

        ConfigurableDataSource dataSource = this.dataSources.get( name );
        if( dataSource == null )
        {
            dataSource = new ConfigurableDataSource( name );
            this.dataSources.put( name, dataSource );
        }

        dataSource.configure( cfg );
    }

    @PathWatcher(value = DATASOURCE_FILES, events = DELETED)
    synchronized void removeDataSource( @Nonnull Path file )
    {
        String fileName = file.getFileName().toString();
        String name = fileName.substring( 0, fileName.length() - ".properties".length() );

        ConfigurableDataSource dataSource = this.dataSources.remove( name );
        if( dataSource != null )
        {
            dataSource.dispose();
        }
    }

    @PreDestroy
    synchronized void destroy()
    {
        for( ConfigurableDataSource dataSource : this.dataSources.values() )
        {
            dataSource.dispose();
        }
    }
}
