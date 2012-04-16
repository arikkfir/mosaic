package org.mosaic.server.db.impl;

import com.jolbox.bonecp.BoneCP;
import java.sql.SQLException;
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
import org.springframework.stereotype.Component;

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
    public synchronized void addDataSourceConfiguration( Configuration configuration ) throws SQLException {
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

    private class DataSourceEntry {

        private final TransactionalDataSource dataSource;

        private final ServiceRegistration<DataSource> registration;

        private DataSourceEntry( Configuration configuration ) throws SQLException {
            AutoBoneCPConfig boneCPConfig = new AutoBoneCPConfig( configuration, bundleContext );
            BoneCP boneConnectionPool = new BoneCP( boneCPConfig );
            this.dataSource = new TransactionalDataSource( boneConnectionPool );

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put( "name", configuration.getName() );
            this.registration = bundleContext.registerService( DataSource.class, dataSource, properties );
        }

        private void unregister() {
            LOG.info( "Shutting down connection pool '{}'", this.dataSource.getPoolName() );
            try {
                this.registration.unregister();
            } catch( IllegalStateException ignore ) {
            }
            this.dataSource.shutdown();
        }
    }
}
