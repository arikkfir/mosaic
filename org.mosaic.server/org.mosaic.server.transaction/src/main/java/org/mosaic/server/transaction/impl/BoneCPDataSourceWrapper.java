package org.mosaic.server.transaction.impl;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author arik
 */
public class BoneCPDataSourceWrapper implements DataSource, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger( BoneCPDataSourceWrapper.class );

    private final String name;

    private final JdbcDriverRegistrar jdbcDriverRegistrar;

    private PrintWriter printWriter;

    private OsgiBoneCP pool;

    public BoneCPDataSourceWrapper( String name, JdbcDriverRegistrar jdbcDriverRegistrar )
    {
        this.name = name;
        this.jdbcDriverRegistrar = jdbcDriverRegistrar;
    }

    public String getPoolName()
    {
        return this.name;
    }

    public void init( MapAccessor<String, String> c )
    {
        BoneCPConfig bcpConfig = new BoneCPConfig();
        bcpConfig.setAcquireIncrement( c.get( "acquireIncrement", Integer.class, 1 ) );
        bcpConfig.setAcquireRetryAttempts( c.get( "acquireRetryAttempts", Integer.class, 0 ) );
        bcpConfig.setAcquireRetryDelay( c.get( "acquireRetryDelay", Long.class, 1000l ), MILLISECONDS );
        bcpConfig.setClassLoader( getClass().getClassLoader() );
        bcpConfig.setCloseConnectionWatch( c.get( "closeConnectionWatch", Boolean.class, false ) );
        bcpConfig.setCloseConnectionWatchTimeout( c.get( "closeConnectionWatchTimeout", Long.class, 1000 * 60l ), MILLISECONDS );
        bcpConfig.setConnectionTestStatement( c.get( "connectionTestStatement" ) );
        bcpConfig.setConnectionTimeout( c.get( "connectionTimeout", Long.class, 1000l * 10 ), MILLISECONDS );
        bcpConfig.setDefaultAutoCommit( c.get( "defaultAutoCommit", Boolean.class, false ) );
        bcpConfig.setDefaultCatalog( c.get( "defaultCatalog" ) );
        bcpConfig.setDefaultReadOnly( c.get( "defaultReadOnly", Boolean.class, false ) );
        bcpConfig.setDefaultTransactionIsolation( c.get( "defaultTransactionIsolation", "SERIALIZABLE" ) );
        bcpConfig.setDisableConnectionTracking( c.get( "disableConnectionTracking", Boolean.class, false ) );
        bcpConfig.setDisableJMX( c.get( "disableJmx", Boolean.class, true ) );
        bcpConfig.setIdleConnectionTestPeriod( c.get( "idleConnectionTestPeriod", Long.class, 1000 * 10l ), MILLISECONDS );
        bcpConfig.setIdleMaxAge( c.get( "idleMaxAge", Long.class, 1000 * 60 * 5l ), MILLISECONDS );
        bcpConfig.setInitSQL( c.get( "initSql" ) );
        bcpConfig.setJdbcUrl( c.require( "url" ) );
        bcpConfig.setLazyInit( true );
        bcpConfig.setLogStatementsEnabled( c.get( "logStatements", Boolean.class, false ) );
        bcpConfig.setMaxConnectionsPerPartition( c.get( "maxConnectionsPerPartition", Integer.class, 5 ) );
        bcpConfig.setMinConnectionsPerPartition( c.get( "minConnectionsPerPartition", Integer.class, 0 ) );
        bcpConfig.setPartitionCount( c.get( "partitionCount", Integer.class, 1 ) );
        bcpConfig.setPassword( c.get( "password" ) );
        bcpConfig.setPoolAvailabilityThreshold( c.get( "poolAvailabilityThreshold", Integer.class, 0 ) );
        bcpConfig.setPoolName( this.name );
        bcpConfig.setQueryExecuteTimeLimit( c.get( "queryExecuteTimeLimit", Long.class, 1000 * 60l ), MILLISECONDS );
        bcpConfig.setReleaseHelperThreads( c.get( "releaseHelperThreads", Integer.class, 0 ) );
        bcpConfig.setServiceOrder( c.get( "serviceOrder", "FIFO" ) );
        bcpConfig.setStatementReleaseHelperThreads( c.get( "statementReleaseHelperThreads", Integer.class, 0 ) );
        bcpConfig.setStatementsCacheSize( c.get( "statementsCacheSize", Integer.class, 10 ) );
        bcpConfig.setStatisticsEnabled( c.get( "statisticsEnabled", Boolean.class, true ) );
        bcpConfig.setTransactionRecoveryEnabled( c.get( "transactionRecoveryEnabled", Boolean.class, false ) );
        bcpConfig.setUsername( c.get( "username" ) );

        // attempt to open a new pool
        OsgiBoneCP newPool;
        try
        {
            newPool = new OsgiBoneCP( bcpConfig );
        }
        catch( SQLException e )
        {
            LOG.error( "Could not create/update connection '{}': {}", this.name, e.getMessage(), e );
            return;
        }

        // new pool created successfully - close our old pool and replace it with the new
        // no exception is thrown from the 'close' method, so we won't get stuck with an old closed pool here
        close();
        this.pool = newPool;
    }

    @Override
    public void close()
    {
        if( this.pool != null )
        {
            try
            {
                this.pool.close();
            }
            catch( Exception e )
            {
                LOG.warn( "Could not close JDBC connection pool '{}': {}", this.name, e.getMessage(), e );
            }
        }
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException( "Mosaic data source uses SLF4J and not Java-Util-Logging" );
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        assertInitialized();
        return this.pool.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException
    {
        LOG.warn( "Mosaic data sources username/password settings are set in data source configuration file - ignoring username/password parameters" );
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return this.printWriter;
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException
    {
        this.printWriter = out;
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException
    {
        if( BoneCP.class.isAssignableFrom( iface ) )
        {
            return iface.cast( this.pool );
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException
    {
        return BoneCP.class.isAssignableFrom( iface );
    }

    @Override
    public String toString()
    {
        return "MosaicDataSource[name=" + this.name + "]";
    }

    private void assertInitialized()
    {
        if( this.pool == null )
        {
            throw new IllegalStateException( "DataSource '" + this.name + "' is not available" );
        }
    }

    private class OsgiBoneCP extends BoneCP
    {
        private OsgiBoneCP( BoneCPConfig config ) throws SQLException
        {
            super( config );
        }

        @Override
        protected Connection obtainRawInternalConnection() throws SQLException
        {
            Connection result =
                    jdbcDriverRegistrar.getConnection( getConfig().getJdbcUrl(), getConfig().getUsername(), getConfig().getPassword(), getConfig().getDriverProperties() );
            if( getConfig().getDefaultAutoCommit() != null )
            {
                result.setAutoCommit( getConfig().getDefaultAutoCommit() );
            }
            if( getConfig().getDefaultReadOnly() != null )
            {
                result.setReadOnly( getConfig().getDefaultReadOnly() );
            }
            if( getConfig().getDefaultCatalog() != null )
            {
                result.setCatalog( getConfig().getDefaultCatalog() );
            }
            return result;
        }
    }
}
