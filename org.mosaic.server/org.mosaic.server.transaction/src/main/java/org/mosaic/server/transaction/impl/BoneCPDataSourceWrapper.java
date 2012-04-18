package org.mosaic.server.transaction.impl;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.mosaic.config.Configuration;
import org.mosaic.logging.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author arik
 */
public class BoneCPDataSourceWrapper implements DataSource, Closeable {

    @SuppressWarnings( "UnusedDeclaration" )
    public static enum TransactionIsolation {
        NONE,
        READ_COMMITTED,
        READ_UNCOMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }

    private static final org.mosaic.logging.Logger LOG = LoggerFactory.getLogger( BoneCPDataSourceWrapper.class );

    private final String name;

    private final JdbcDriverRegistrar jdbcDriverRegistrar;

    private PrintWriter printWriter;

    private OsgiBoneCP pool;

    public BoneCPDataSourceWrapper( String name, JdbcDriverRegistrar jdbcDriverRegistrar ) {
        this.name = name;
        this.jdbcDriverRegistrar = jdbcDriverRegistrar;
    }

    public String getPoolName() {
        return this.name;
    }

    public void init( Configuration cfg ) {
        BoneCPConfig bcpConfig = new BoneCPConfig();
        bcpConfig.setAcquireIncrement( ri( cfg, "acquireIncrement", 1 ) );
        bcpConfig.setAcquireRetryAttempts( ri( cfg, "acquireRetryAttempts", 0 ) );
        bcpConfig.setAcquireRetryDelay( rl( cfg, "acquireRetryDelay", 1000 ), MILLISECONDS );
        bcpConfig.setClassLoader( getClass().getClassLoader() );
        bcpConfig.setCloseConnectionWatch( rb( cfg, "closeConnectionWatch", false ) );
        bcpConfig.setCloseConnectionWatchTimeout( rl( cfg, "closeConnectionWatchTimeout", 1000 * 60 ), MILLISECONDS );
        bcpConfig.setConnectionTestStatement( gs( cfg, "connectionTestStatement" ) );
        bcpConfig.setConnectionTimeout( rl( cfg, "connectionTimeout", 1000 * 10 ), MILLISECONDS );
        bcpConfig.setDefaultAutoCommit( rb( cfg, "defaultAutoCommit", false ) );
        bcpConfig.setDefaultCatalog( gs( cfg, "defaultCatalog" ) );
        bcpConfig.setDefaultReadOnly( rb( cfg, "defaultReadOnly", false ) );
        bcpConfig.setDefaultTransactionIsolation( re( cfg, "defaultTransactionIsolation", TransactionIsolation.class, TransactionIsolation.SERIALIZABLE ).name() );
        bcpConfig.setDisableConnectionTracking( rb( cfg, "disableConnectionTracking", false ) );
        bcpConfig.setDisableJMX( rb( cfg, "disableJmx", true ) );
        bcpConfig.setIdleConnectionTestPeriod( rl( cfg, "idleConnectionTestPeriod", 1000 * 10 ), MILLISECONDS );
        bcpConfig.setIdleMaxAge( rl( cfg, "idleMaxAge", 1000 * 60 * 5 ), MILLISECONDS );
        bcpConfig.setInitSQL( gs( cfg, "initSql" ) );
        bcpConfig.setJdbcUrl( rs( cfg, "url" ) );
        bcpConfig.setLazyInit( true );
        bcpConfig.setLogStatementsEnabled( rb( cfg, "logStatements", false ) );
        bcpConfig.setMaxConnectionsPerPartition( ri( cfg, "maxConnectionsPerPartition", 5 ) );
        bcpConfig.setMinConnectionsPerPartition( ri( cfg, "minConnectionsPerPartition", 0 ) );
        bcpConfig.setPartitionCount( ri( cfg, "partitionCount", 1 ) );
        bcpConfig.setPassword( gs( cfg, "password" ) );
        bcpConfig.setPoolAvailabilityThreshold( ri( cfg, "poolAvailabilityThreshold", 0 ) );
        bcpConfig.setPoolName( cfg.getName() );
        bcpConfig.setQueryExecuteTimeLimit( rl( cfg, "queryExecuteTimeLimit", 1000 * 60 ), MILLISECONDS );
        bcpConfig.setReleaseHelperThreads( ri( cfg, "releaseHelperThreads", 0 ) );
        bcpConfig.setServiceOrder( rs( cfg, "serviceOrder", "FIFO" ) );
        bcpConfig.setStatementReleaseHelperThreads( ri( cfg, "statementReleaseHelperThreads", 0 ) );
        bcpConfig.setStatementsCacheSize( ri( cfg, "statementsCacheSize", 10 ) );
        bcpConfig.setStatisticsEnabled( rb( cfg, "statisticsEnabled", true ) );
        bcpConfig.setTransactionRecoveryEnabled( rb( cfg, "transactionRecoveryEnabled", false ) );
        bcpConfig.setUsername( gs( cfg, "username" ) );

        // attempt to open a new pool
        OsgiBoneCP newPool;
        try {
            newPool = new OsgiBoneCP( bcpConfig );
        } catch( SQLException e ) {
            LOG.error( "Could not create/update connection '{}': {}", this.name, e.getMessage(), e );
            return;
        }

        // new pool created successfully - close our old pool and replace it with the new
        // no exception is thrown from the 'close' method, so we won't get stuck with an old closed pool here
        close();
        this.pool = newPool;
    }

    @Override
    public void close() {
        if( this.pool != null ) {
            try {
                this.pool.close();
            } catch( Exception e ) {
                LOG.warn( "Could not close JDBC connection pool '{}': {}", this.name, e.getMessage(), e );
            }
        }
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException( "Mosaic data source uses SLF4J and not Java-Util-Logging" );
    }

    @Override
    public Connection getConnection() throws SQLException {
        assertInitialized();
        return this.pool.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException {
        LOG.warn( "Mosaic data sources username/password settings are set in data source configuration file - ignoring username/password parameters" );
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.printWriter;
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException {
        this.printWriter = out;
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if( BoneCP.class.isAssignableFrom( iface ) ) {
            return iface.cast( this.pool );
        } else {
            return null;
        }
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException {
        return BoneCP.class.isAssignableFrom( iface );
    }

    @Override
    public String toString() {
        return "MosaicDataSource[name=" + this.name + "]";
    }

    private void assertInitialized() {
        if( this.pool == null ) {
            throw new IllegalStateException( "DataSource '" + this.name + "' is not available" );
        }
    }

    private int ri( Configuration cfg, String key, int defaultValue ) {
        return cfg.require( key, Integer.class, defaultValue );
    }

    private long rl( Configuration cfg, String key, long defaultValue ) {
        return cfg.require( key, Long.class, defaultValue );
    }

    private boolean rb( Configuration cfg, String key, boolean defaultValue ) {
        return cfg.require( key, Boolean.class, defaultValue );
    }

    private String rs( Configuration cfg, String key ) {
        return cfg.require( key, String.class, null );
    }

    private String rs( Configuration cfg, String key, String defaultValue ) {
        return cfg.require( key, String.class, defaultValue );
    }

    private String gs( Configuration cfg, String key ) {
        return cfg.get( key, String.class );
    }

    private <T extends Enum> T re( Configuration cfg, String key, Class<T> type, T defaultValue ) {
        return cfg.require( key, type, defaultValue );
    }

    private class OsgiBoneCP extends BoneCP {

        private OsgiBoneCP( BoneCPConfig config ) throws SQLException {
            super( config );
        }

        @Override
        protected Connection obtainRawInternalConnection() throws SQLException {
            Connection result = jdbcDriverRegistrar.getConnection( getConfig().getJdbcUrl(),
                                                                   getConfig().getUsername(),
                                                                   getConfig().getPassword(),
                                                                   getConfig().getDriverProperties() );
            if( getConfig().getDefaultAutoCommit() != null ) {
                result.setAutoCommit( getConfig().getDefaultAutoCommit() );
            }
            if( getConfig().getDefaultReadOnly() != null ) {
                result.setReadOnly( getConfig().getDefaultReadOnly() );
            }
            if( getConfig().getDefaultCatalog() != null ) {
                result.setCatalog( getConfig().getDefaultCatalog() );
            }
            return result;
        }
    }
}
