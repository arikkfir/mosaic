package org.mosaic.datasource.impl;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.Property;
import org.mosaic.modules.ServiceRegistration;
import org.mosaic.util.collections.MapEx;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class ConfigurableDataSource implements DataSource
{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger( ConfigurableDataSource.class );

    static final String TX_CONNECTION_KEY = ConfigurableDataSource.class.getName() + "#connection";

    @Nonnull
    private final String name;

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Component
    private TransactionManagerImpl transactionManager;

    @Nullable
    private ComboPooledDataSource dataSource;

    @Nullable
    private ServiceRegistration<DataSource> registration;

    ConfigurableDataSource( @Nonnull String name )
    {
        this.name = name;
    }

    @Nonnull
    @Override
    public Connection getConnection() throws SQLException
    {
        ComboPooledDataSource dataSource = this.dataSource;
        if( dataSource == null )
        {
            throw new IllegalStateException( "data source '" + this.name + "' not available" );
        }

        TransactionManagerImpl.TransactionImpl transaction = this.transactionManager.getTransaction();
        if( transaction == null )
        {
            throw new IllegalStateException( "no active transaction" );
        }

        Connection connection = transaction.getAttributes().get( TX_CONNECTION_KEY, Connection.class );
        if( connection == null )
        {
            connection = dataSource.getConnection();
            try
            {
                connection.setAutoCommit( false );
                connection.setHoldability( ResultSet.CLOSE_CURSORS_AT_COMMIT );
                connection.setReadOnly( transaction.isReadOnly() );
                connection.setTransactionIsolation( Connection.TRANSACTION_SERIALIZABLE );
            }
            catch( Exception e )
            {
                LOG.error( "Could not initialize SQL connection after checking out from the pool (will return to pool): {}", e.getMessage(), e );
                try
                {
                    connection.close();
                }
                catch( Exception e1 )
                {
                    LOG.warn( "Could not close SQL connection (attempted close due to error in initialization): {}", e1.getMessage(), e1 );
                }
                throw e;
            }
            transaction.getAttributes().put( TX_CONNECTION_KEY, connection );
        }

        return connection;
    }

    @Nonnull
    @Override
    public Connection getConnection( String username, String password ) throws SQLException
    {
        throw new SQLFeatureNotSupportedException( "custom username/password are not supported" );
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return getDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException
    {
        getDataSource().setLogWriter( out );
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return getDataSource().getLoginTimeout();
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException
    {
        getDataSource().setLoginTimeout( seconds );
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return java.util.logging.Logger.getLogger( "org.mosaic.datasource" );
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException
    {
        throw new SQLFeatureNotSupportedException( "'unwrap' operation not supported for data source" );
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException
    {
        return false;
    }

    void configure( @Nonnull MapEx<String, String> cfg )
    {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDataSourceName( this.name );
        dataSource.setDescription( cfg.get( "description" ) );
        dataSource.setJdbcUrl( cfg.require( "url" ) );
        dataSource.setUser( cfg.get( "username" ) );
        dataSource.setPassword( cfg.get( "password" ) );
        dataSource.setCheckoutTimeout( cfg.get( "checkoutTimeoutSeconds", Integer.class, 5 ) * 1000 );
        dataSource.setAcquireIncrement( cfg.get( "acquireIncrement", Integer.class, 1 ) );
        dataSource.setAcquireRetryAttempts( cfg.get( "acquireRetryAttempts", Integer.class, 30 ) );
        dataSource.setAcquireRetryDelay( cfg.get( "acquireRetryDelaySeconds", Integer.class, 1 ) * 1000 );
        dataSource.setAutoCommitOnClose( cfg.get( "autoCommitOnClose", Boolean.class, false ) );
        dataSource.setIdleConnectionTestPeriod( cfg.get( "idleConnectionTestPeriod", Integer.class, 300 ) );
        dataSource.setInitialPoolSize( cfg.get( "initialPoolSize", Integer.class, 1 ) );
        dataSource.setMaxIdleTime( cfg.get( "maxIdleTimeSeconds", Integer.class, 300 ) );
        dataSource.setMaxPoolSize( cfg.get( "maxPoolSize", Integer.class, 5 ) );
        dataSource.setMaxStatements( cfg.get( "globalMaxStatements", Integer.class, 0 ) );
        dataSource.setMaxStatementsPerConnection( cfg.get( "maxStatementsPerConnection", Integer.class, 100 ) );
        dataSource.setMinPoolSize( cfg.get( "minPoolSize", Integer.class, 1 ) );
        dataSource.setTestConnectionOnCheckout( cfg.get( "testConnectionOnCheckout", Boolean.class, false ) );
        dataSource.setTestConnectionOnCheckin( cfg.get( "testConnectionOnCheckin", Boolean.class, false ) );
        dataSource.setPreferredTestQuery( cfg.get( "testQuery" ) );
        dataSource.setMaxIdleTimeExcessConnections( cfg.get( "maxIdleTimeExcessConnectionsSeconds", Integer.class, 300 ) );
        dataSource.setMaxConnectionAge( cfg.get( "maxConnectionAge", Integer.class, 0 ) );
        dataSource.setUnreturnedConnectionTimeout( cfg.get( "unreturnedConnectionTimeoutSeconds", Integer.class, 0 ) );
        dataSource.setDebugUnreturnedConnectionStackTraces( cfg.get( "debugUnreturnedConnectionStackTraces", Boolean.class, false ) );
        dataSource.setStatementCacheNumDeferredCloseThreads( cfg.get( "statementCacheNumDeferredCloseThreads", Integer.class, 0 ) );

        Properties properties = new Properties();
        for( Map.Entry<String, String> entry : cfg.entrySet() )
        {
            if( entry.getKey().startsWith( "property." ) )
            {
                properties.put( entry.getKey().substring( "property.".length() ), entry.getValue() );
            }
        }
        dataSource.setProperties( properties );

        //noinspection UnusedDeclaration
        try( Connection connection = dataSource.getConnection() )
        {
            this.dataSource = dataSource;
            this.registration = this.module.getModuleWiring().register( DataSource.class, this, Property.property( "name", this.name ) );
        }
        catch( Exception e )
        {
            // no-op
        }
    }

    void dispose()
    {
        if( this.registration != null )
        {
            this.registration.unregister();
            this.registration = null;
        }

        if( this.dataSource != null )
        {
            try
            {
                this.dataSource.close();
            }
            catch( Exception e )
            {
                LOG.warn( "Could not close data source '{}': {}", this.name, e.getMessage(), e );
            }
        }
    }

    @Nonnull
    private DataSource getDataSource()
    {
        ComboPooledDataSource dataSource = this.dataSource;
        if( dataSource == null )
        {
            throw new IllegalStateException( "data source not available" );
        }
        return dataSource;
    }
}
