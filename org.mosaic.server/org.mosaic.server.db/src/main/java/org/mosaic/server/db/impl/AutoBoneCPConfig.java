package org.mosaic.server.db.impl;

import com.jolbox.bonecp.BoneCPConfig;
import org.mosaic.config.Configuration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author arik
 */
public class AutoBoneCPConfig extends BoneCPConfig {

    private final Configuration cfg;

    public AutoBoneCPConfig( Configuration cfg, BundleContext bundleContext ) {
        this.cfg = cfg;
        setAcquireIncrement( ri( "acquireIncrement", 2 ) );
        setAcquireRetryAttempts( ri( "acquireRetryAttempts", 3 ) );
        setAcquireRetryDelay( rl( "acquireRetryDelay", 1000 ), MILLISECONDS );
        setClassLoader( bundleContext.getBundle().adapt( BundleWiring.class ).getClassLoader() );
        setCloseConnectionWatch( rb( "closeConnectionWatch", false ) );
        setCloseConnectionWatchTimeout( rl( "closeConnectionWatchTimeout", 1000 * 60 ), MILLISECONDS );
        setConnectionTestStatement( gs( "connectionTestStatement" ) );
        setConnectionTimeout( rl( "connectionTimeout", 1000 * 10 ), MILLISECONDS );
        setDefaultAutoCommit( rb( "defaultAutoCommit", false ) );
        setDefaultCatalog( gs( "defaultCatalog" ) );
        setDefaultReadOnly( rb( "defaultReadOnly", false ) );
        setDefaultTransactionIsolation( re( "defaultTransactionIsolation", TransactionIsolation.class, TransactionIsolation.SERIALIZABLE ).name() );
        setDisableConnectionTracking( rb( "disableConnectionTracking", false ) );
        setDisableJMX( rb( "disableJmx", true ) );
        setIdleConnectionTestPeriod( rl( "idleConnectionTestPeriod", 1000 * 10 ), MILLISECONDS );
        setIdleMaxAge( rl( "idleMaxAge", 1000 * 60 * 5 ), MILLISECONDS );
        setInitSQL( gs( "initSql" ) );
        setJdbcUrl( rs( "url" ) );
        setLogStatementsEnabled( rb( "logStatements", false ) );
        setMaxConnectionsPerPartition( ri( "maxConnectionsPerPartition", 15 ) );
        setMinConnectionsPerPartition( ri( "minConnectionsPerPartition", 1 ) );
        setPartitionCount( ri( "partitionCount", 1 ) );
        setPassword( gs( "password" ) );
        setPoolAvailabilityThreshold( ri( "poolAvailabilityThreshold", 20 ) );
        setPoolName( cfg.getName() );
        setQueryExecuteTimeLimit( rl( "queryExecuteTimeLimit", 1000 * 60 ), MILLISECONDS );
        setReleaseHelperThreads( ri( "releaseHelperThreads", 0 ) );
        setServiceOrder( rs( "serviceOrder", "FIFO" ) );
        setStatementReleaseHelperThreads( ri( "statementReleaseHelperThreads", 0 ) );
        setStatementsCacheSize( ri( "statementsCacheSize", 10 ) );
        setStatisticsEnabled( rb( "statisticsEnabled", true ) );
        setTransactionRecoveryEnabled( rb( "transactionRecoveryEnabled", false ) );
        setUsername( gs( "username" ) );
    }

    private int ri( String key, int defaultValue ) {
        return this.cfg.require( key, Integer.class, defaultValue );
    }

    private long rl( String key, long defaultValue ) {
        return this.cfg.require( key, Long.class, defaultValue );
    }

    private boolean rb( String key, boolean defaultValue ) {
        return this.cfg.require( key, Boolean.class, defaultValue );
    }

    private String rs( String key ) {
        return this.cfg.require( key, String.class, null );
    }

    private String rs( String key, String defaultValue ) {
        return this.cfg.require( key, String.class, defaultValue );
    }

    private String gs( String key ) {
        return this.cfg.get( key, String.class );
    }

    private <T extends Enum> T re( String key, Class<T> type, T defaultValue ) {
        return this.cfg.require( key, type, defaultValue );
    }
}
