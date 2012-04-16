package org.mosaic.server.db.impl;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.BundleContextAware;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author arik
 */
@Component
public class DataSourceManager implements BundleContextAware {

    private static final Logger LOG = LoggerFactory.getLogger( DataSourceManager.class );

    public static final String DATASOURCE_FILTER = "name=*-ds";

    private final Map<String, DataSourceEntry> pools = new ConcurrentHashMap<>();

    private BundleContext bundleContext;

    @Override
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    @ServiceBind( filter = DATASOURCE_FILTER )
    public synchronized void addDataSourceConfiguration( Configuration configuration ) {
        this.pools.put( configuration.getName(), new DataSourceEntry( configuration ) );
    }

    @ServiceUnbind( filter = DATASOURCE_FILTER )
    public synchronized void removeDataSourceConfiguration( Configuration configuration ) {
        DataSourceEntry removed = this.pools.remove( configuration.getName() );
        if( removed != null ) {
            removed.unregister();
        }
    }

    @PreDestroy
    public synchronized void destroy() {
        for( DataSourceEntry dataSource : this.pools.values() ) {
            dataSource.unregister();
        }
        this.pools.clear();
    }

    private BoneCPConfig createPool( Configuration cfg ) {
        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement( ri( cfg, "acquireIncrement", 2 ) );
        config.setAcquireRetryAttempts( ri( cfg, "acquireRetryAttempts", 3 ) );
        config.setAcquireRetryDelay( rl( cfg, "acquireRetryDelay", 1000 ), MILLISECONDS );
        config.setClassLoader( this.bundleContext.getBundle().adapt( BundleWiring.class ).getClassLoader() );
        config.setCloseConnectionWatch( rb( cfg, "closeConnectionWatch", false ) );
        config.setCloseConnectionWatchTimeout( rl( cfg, "closeConnectionWatchTimeout", 1000 * 60 ), MILLISECONDS );
        config.setConnectionTestStatement( gs( cfg, "connectionTestStatement" ) );
        config.setConnectionTimeout( rl( cfg, "connectionTimeout", 1000 * 10 ), MILLISECONDS );
        config.setDefaultAutoCommit( rb( cfg, "defaultAutoCommit", false ) );
        config.setDefaultCatalog( gs( cfg, "defaultCatalog" ) );
        config.setDefaultReadOnly( rb( cfg, "defaultReadOnly", false ) );
        config.setDefaultTransactionIsolation( re( cfg, "defaultTransactionIsolation", TransactionIsolation.class, TransactionIsolation.SERIALIZABLE ).name() );
        config.setDisableConnectionTracking( rb( cfg, "disableConnectionTracking", false ) );
        config.setDisableJMX( rb( cfg, "disableJmx", true ) );
        config.setIdleConnectionTestPeriod( rl( cfg, "idleConnectionTestPeriod", 1000 * 10 ), MILLISECONDS );
        config.setIdleMaxAge( rl( cfg, "idleMaxAge", 1000 * 60 * 5 ), MILLISECONDS );
        config.setInitSQL( gs( cfg, "initSql" ) );
        config.setJdbcUrl( rs( cfg, "url" ) );
        config.setLogStatementsEnabled( rb( cfg, "logStatements", false ) );
        config.setMaxConnectionsPerPartition( ri( cfg, "maxConnectionsPerPartition", 15 ) );
        config.setMinConnectionsPerPartition( ri( cfg, "minConnectionsPerPartition", 1 ) );
        config.setPartitionCount( ri( cfg, "partitionCount", 1 ) );
        config.setPassword( gs( cfg, "password" ) );
        config.setPoolAvailabilityThreshold( ri( cfg, "poolAvailabilityThreshold", 20 ) );
        config.setPoolName( cfg.getName() );
        config.setQueryExecuteTimeLimit( rl( cfg, "queryExecuteTimeLimit", 1000 * 60 ), MILLISECONDS );
        config.setReleaseHelperThreads( ri( cfg, "releaseHelperThreads", 0 ) );
        config.setServiceOrder( rs( cfg, "serviceOrder", "FIFO" ) );
        config.setStatementReleaseHelperThreads( ri( cfg, "statementReleaseHelperThreads", 0 ) );
        config.setStatementsCacheSize( ri( cfg, "statementsCacheSize", 10 ) );
        config.setStatisticsEnabled( rb( cfg, "statisticsEnabled", true ) );
        config.setTransactionRecoveryEnabled( rb( cfg, "transactionRecoveryEnabled", false ) );
        config.setUsername( gs( cfg, "username" ) );
        return config;
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

    private class DataSourceEntry {

        private final BoneCPDataSource dataSource;

        private final ServiceRegistration<DataSource> registration;

        private DataSourceEntry( Configuration cfg ) {
            BoneCPConfig poolConfiguration = createPool( cfg );
            this.dataSource = new BoneCPDataSource( poolConfiguration );

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put( "name", cfg.getName() );
            this.registration = bundleContext.registerService( DataSource.class, dataSource, properties );
        }

        private void unregister() {
            LOG.info( "Shutting down connection pool '{}'", this.dataSource.getPoolName() );
            try {
                this.registration.unregister();
            } catch( IllegalStateException ignore ) {
            }
            try {
                this.dataSource.close();
            } catch( Exception e ) {
                LOG.error( "Error shutting down connection pool '{}': {}", this.dataSource.getPoolName(), e.getMessage(), e );
            }
        }
    }
}
