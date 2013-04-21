package org.mosaic.database.tx.impl;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.lifecycle.DP;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleListener;
import org.mosaic.lifecycle.ModuleListenerAdapter;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.reflect.Modifier.isAbstract;

/**
 * @author arik
 */
@Bean
@Service(ModuleListener.class)
public class DataSourceManager extends ModuleListenerAdapter implements JdbcDriverFinder
{
    private static final Logger LOG = LoggerFactory.getLogger( DataSourceManager.class );

    private static final Pattern DATA_SOURCE_PROPERTY_PATTERN = Pattern.compile( "([^\\.]+)\\.(.+)" );

    @Nonnull
    private final Map<Module, Collection<Driver>> drivers = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<String, DataSourceEntry> dataSources = new ConcurrentHashMap<>();

    @Nonnull
    private DataSourceFactory dataSourceFactory;

    @Nonnull
    private Module module;

    @Nonnull
    private ConversionService conversionService;

    @BeanRef
    public void setDataSourceFactory( @Nonnull DataSourceFactory dataSourceFactory )
    {
        this.dataSourceFactory = dataSourceFactory;
    }

    @ModuleRef
    public void setModule( @Nonnull Module module )
    {
        this.module = module;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Configurable("databases")
    public void configure( @Nonnull MapEx<String, String> configuration )
    {
        // split configuration into smaller dataSource-specific configurations
        Map<String, MapEx<String, String>> dataSourceConfigurations = new HashMap<>();
        for( Map.Entry<String, String> entry : configuration.entrySet() )
        {
            Matcher matcher = DATA_SOURCE_PROPERTY_PATTERN.matcher( entry.getKey() );
            if( matcher.matches() )
            {
                String dataSourceName = matcher.group( 1 );

                MapEx<String, String> cfg = dataSourceConfigurations.get( dataSourceName );
                if( cfg == null )
                {
                    cfg = new HashMapEx<>( 10, this.conversionService );
                    dataSourceConfigurations.put( dataSourceName, cfg );
                }
                cfg.put( matcher.group( 2 ), entry.getValue() );
            }
        }

        // update already-existing data sources, and remove any data sources no longer referenced in the configuration
        Iterator<DataSourceEntry> iterator = this.dataSources.values().iterator();
        while( iterator.hasNext() )
        {
            DataSourceEntry dataSourceEntry = iterator.next();
            if( !dataSourceConfigurations.containsKey( dataSourceEntry.name ) )
            {
                dataSourceEntry.close();
                iterator.remove();
            }
            else
            {
                dataSourceEntry.updateConfiguration( dataSourceConfigurations.get( dataSourceEntry.name ) );
            }
        }

        // add new data sources in the configuration that we do not know about
        for( Map.Entry<String, MapEx<String, String>> entry : dataSourceConfigurations.entrySet() )
        {
            String dataSourceName = entry.getKey();
            if( !this.dataSources.containsKey( entry.getKey() ) )
            {
                this.dataSources.put( dataSourceName, new DataSourceEntry( dataSourceName, entry.getValue() ) );
            }
        }
    }

    @Override
    @Nullable
    public Driver getDriver( @Nonnull String url )
    {
        Map<Module, Collection<Driver>> driversByModule = this.drivers;
        for( Collection<Driver> drivers : driversByModule.values() )
        {
            for( Driver driver : drivers )
            {
                try
                {
                    if( driver.acceptsURL( url ) )
                    {
                        return driver;
                    }
                }
                catch( SQLException ignore )
                {
                    // if driver fails, just treat this as if the driver rejected it and move to the next driver
                }
            }
        }
        return null;
    }

    @Override
    public void moduleActivated( @Nonnull Module module )
    {
        scanForJdbcDrivers( module );
    }

    @Override
    public void moduleDeactivated( @Nonnull Module module )
    {
        removeJdbcDrivers( module );
    }

    private void scanForJdbcDrivers( @Nonnull Module module )
    {
        Map<Module, Collection<Driver>> drivers = this.drivers;

        Collection<Driver> jdbcDriverClasses = new LinkedHashSet<>();
        for( String className : findJdbcDriverClassNames( module ) )
        {
            Driver driver = loadDriver( module, className );
            if( driver != null )
            {
                jdbcDriverClasses.add( driver );
                LOG.info( "Registered JDBC driver '{}' from module '{}'", driver, module );
            }
        }
        if( !jdbcDriverClasses.isEmpty() )
        {
            drivers.put( module, jdbcDriverClasses );

            for( DataSourceEntry dataSourceEntry : this.dataSources.values() )
            {
                dataSourceEntry.jdbcDriversAdded();
            }
        }
    }

    private List<String> findJdbcDriverClassNames( @Nonnull Module module )
    {
        URL driverServiceUrl = module.getResource( "/META-INF/services/java.sql.Driver" );
        if( driverServiceUrl != null )
        {
            try
            {
                return Resources.readLines( driverServiceUrl, Charset.forName( "UTF-8" ) );
            }
            catch( IOException e )
            {
                LOG.warn( "Could not scan JDBC drivers in module '{}': {}", module, e.getMessage(), e );
            }
        }
        return Collections.emptyList();
    }

    private Driver loadDriver( @Nonnull Module module, @Nonnull String className )
    {
        try
        {
            ClassLoader classLoader = module.getClassLoader();
            if( classLoader != null )
            {
                Class<?> cls = classLoader.loadClass( className );
                if( !isAbstract( cls.getModifiers() ) && !cls.isInterface() )
                {
                    if( Driver.class.isAssignableFrom( cls ) )
                    {
                        Class<? extends Driver> driverClass = cls.asSubclass( Driver.class );
                        Constructor<? extends Driver> defaultConstructor = driverClass.getConstructor();
                        return defaultConstructor.newInstance();
                    }
                }
            }
        }
        catch( NoSuchMethodException e )
        {
            // ignore classes without a default constructor
        }
        catch( Exception e )
        {
            LOG.warn( "Could not load JDBC driver '{}' from module '{}': {}", className, module, e.getMessage(), e );
        }
        return null;
    }

    private void removeJdbcDrivers( @Nonnull Module module )
    {
        Map<Module, Collection<Driver>> drivers = this.drivers;
        Collection<Driver> value = drivers.remove( module );
        if( value != null )
        {
            for( DataSourceEntry dataSourceEntry : this.dataSources.values() )
            {
                for( Driver driver : value )
                {
                    dataSourceEntry.jdbcDriverRemoved( driver );
                }
            }
        }
    }

    private class DataSourceEntry implements DataSource
    {
        @Nonnull
        private final String name;

        @Nullable
        private MapEx<String, String> configuration;

        @Nullable
        private DataSourceFactory.ConfigurableDataSource dataSource;

        @Nullable
        private Module.ServiceExport serviceExport;

        private DataSourceEntry( @Nonnull String name, MapEx<String, String> configuration )
        {
            this.name = name;
            updateConfiguration( configuration );
        }

        public synchronized void updateConfiguration( @Nonnull MapEx<String, String> configuration )
        {
            if( this.configuration != null && this.configuration.equals( configuration ) && this.dataSource != null )
            {
                // our data source is already open and configuration hasn't changed
                return;
            }

            // if data source is open, close it first
            close();

            // open with new configuration
            this.configuration = configuration;
            open();
        }

        public synchronized void jdbcDriversAdded()
        {
            if( this.dataSource == null )
            {
                open();
            }
        }

        public synchronized void jdbcDriverRemoved( @Nonnull Driver driver )
        {
            if( this.dataSource != null && this.dataSource.getDriver() == driver )
            {
                reopen();
            }
        }

        public synchronized void open()
        {
            if( this.configuration != null )
            {
                this.dataSource = dataSourceFactory.createDataSource( this.name, DataSourceManager.this, this.configuration );
                if( this.dataSource != null )
                {
                    this.serviceExport = module.exportService( DataSource.class, this, DP.dp( "name", this.name ) );
                }
            }
        }

        public synchronized void reopen()
        {
            close();
            open();
        }

        public synchronized void close()
        {
            if( this.serviceExport != null )
            {
                this.serviceExport.unregister();
            }

            if( this.dataSource != null )
            {
                try
                {
                    this.dataSource.close();
                }
                catch( Exception e )
                {
                    LOG.warn( "Error while closing data source '{}': {}", this.name, e.getMessage(), e );
                }
                this.dataSource = null;
            }
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            final DataSourceFactory.ConfigurableDataSource dataSource = this.dataSource;
            if( dataSource == null )
            {
                throw new IllegalStateException( "Data source '" + this.name + "' is currently not available" );
            }
            else
            {
                return dataSource.getConnection();
            }
        }

        @Override
        public Connection getConnection( String username, String password ) throws SQLException
        {
            throw new UnsupportedOperationException( "Please configure database authentication via the 'databses' configuration" );
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            // disable log writer
            return null;
        }

        @Override
        public void setLogWriter( PrintWriter out ) throws SQLException
        {
            throw new UnsupportedOperationException( "LogWriter feature for Mosaic data-sources is disabled" );
        }

        @Override
        public int getLoginTimeout() throws SQLException
        {
            DataSourceFactory.ConfigurableDataSource dataSource = this.dataSource;
            if( dataSource == null )
            {
                throw new IllegalStateException( "Data source '" + this.name + "' is currently not available" );
            }
            else
            {
                return dataSource.getLoginTimeout();
            }
        }

        @Override
        public void setLoginTimeout( int seconds ) throws SQLException
        {
            throw new UnsupportedOperationException( "Please configure database login timeout via the 'databses' configuration" );
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap( Class<T> iface ) throws SQLException
        {
            DataSourceFactory.ConfigurableDataSource dataSource = this.dataSource;
            if( dataSource == null )
            {
                throw new IllegalStateException( "Data source '" + this.name + "' is currently not available" );
            }
            else if( DataSourceFactory.ConfigurableDataSource.class.equals( iface ) )
            {
                return iface.cast( this.dataSource );
            }
            else
            {
                return this.dataSource.unwrap( iface );
            }
        }

        @Override
        public boolean isWrapperFor( Class<?> iface ) throws SQLException
        {
            DataSourceFactory.ConfigurableDataSource dataSource = this.dataSource;
            if( dataSource == null )
            {
                throw new IllegalStateException( "Data source '" + this.name + "' is currently not available" );
            }
            else
            {
                return DataSourceFactory.ConfigurableDataSource.class.equals( iface ) || this.dataSource.isWrapperFor( iface );
            }
        }
    }
}
