package org.mosaic.datasource.impl;

import com.google.common.base.Optional;
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

        Optional<Connection> conHolder = transaction.getAttributes().find( TX_CONNECTION_KEY, Connection.class );
        if( !conHolder.isPresent() )
        {
            Connection connection = dataSource.getConnection();
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
            return connection;
        }
        else
        {
            return conHolder.get();
        }
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
        dataSource.setJdbcUrl( cfg.find( "url" ).get() );
        dataSource.setUser( cfg.get( "username" ) );
        dataSource.setPassword( cfg.get( "password" ) );
        dataSource.setCheckoutTimeout( cfg.find( "checkoutTimeoutSeconds", Integer.class ).or( 5 ) * 1000 );
        dataSource.setAcquireIncrement( cfg.find( "acquireIncrement", Integer.class ).or( 1 ) );
        dataSource.setAcquireRetryAttempts( cfg.find( "acquireRetryAttempts", Integer.class ).or( 30 ) );
        dataSource.setAcquireRetryDelay( cfg.find( "acquireRetryDelaySeconds", Integer.class ).or( 1 ) * 1000 );
        dataSource.setAutoCommitOnClose( cfg.find( "autoCommitOnClose", Boolean.class ).or( false ) );
        dataSource.setIdleConnectionTestPeriod( cfg.find( "idleConnectionTestPeriod", Integer.class ).or( 300 ) );
        dataSource.setInitialPoolSize( cfg.find( "initialPoolSize", Integer.class ).or( 1 ) );
        dataSource.setMaxIdleTime( cfg.find( "maxIdleTimeSeconds", Integer.class ).or( 300 ) );
        dataSource.setMaxPoolSize( cfg.find( "maxPoolSize", Integer.class ).or( 5 ) );
        dataSource.setMaxStatements( cfg.find( "globalMaxStatements", Integer.class ).or( 0 ) );
        dataSource.setMaxStatementsPerConnection( cfg.find( "maxStatementsPerConnection", Integer.class ).or( 100 ) );
        dataSource.setMinPoolSize( cfg.find( "minPoolSize", Integer.class ).or( 1 ) );
        dataSource.setTestConnectionOnCheckout( cfg.find( "testConnectionOnCheckout", Boolean.class ).or( false ) );
        dataSource.setTestConnectionOnCheckin( cfg.find( "testConnectionOnCheckin", Boolean.class ).or( false ) );
        dataSource.setPreferredTestQuery( cfg.find( "testQuery" ).orNull() );
        dataSource.setMaxIdleTimeExcessConnections( cfg.find( "maxIdleTimeExcessConnectionsSeconds", Integer.class ).or( 300 ) );
        dataSource.setMaxConnectionAge( cfg.find( "maxConnectionAge", Integer.class ).or( 0 ) );
        dataSource.setUnreturnedConnectionTimeout( cfg.find( "unreturnedConnectionTimeoutSeconds", Integer.class ).or( 0 ) );
        dataSource.setDebugUnreturnedConnectionStackTraces( cfg.find( "debugUnreturnedConnectionStackTraces", Boolean.class ).or( false ) );
        dataSource.setStatementCacheNumDeferredCloseThreads( cfg.find( "statementCacheNumDeferredCloseThreads", Integer.class ).or( 0 ) );

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
            this.registration = this.module.register( DataSource.class, this, Property.property( "name", this.name ) );
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
