package org.mosaic.server.transaction.impl;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class DataSourceManager {

    public static final String DATASOURCE_FILTER = "name=*-ds";

    private final Map<String, TransactionManagerImpl> txManagers = new HashMap<>();

    private JdbcDriverRegistrar jdbcDriverRegistrar;

    @Autowired
    public void setJdbcDriverRegistrar( JdbcDriverRegistrar jdbcDriverRegistrar ) {
        this.jdbcDriverRegistrar = jdbcDriverRegistrar;
    }

    @ServiceBind( filter = DATASOURCE_FILTER )
    public synchronized void addDataSourceConfiguration( Configuration configuration ) throws SQLException {
        TransactionManagerImpl txMgr = this.txManagers.get( configuration.getName() );
        if( txMgr == null ) {
            txMgr = new TransactionManagerImpl( configuration.getName(), this.jdbcDriverRegistrar );
            this.txManagers.put( configuration.getName(), txMgr );
        }
        txMgr.updateConfiguration( configuration );
    }

    @ServiceUnbind( filter = DATASOURCE_FILTER )
    public synchronized void removeDataSourceConfiguration( Configuration configuration ) {
        TransactionManagerImpl removed = this.txManagers.remove( configuration.getName() );
        if( removed != null ) {
            removed.destroy();
        }
    }

    @PreDestroy
    public synchronized void destroy() throws SQLException {
        for( TransactionManagerImpl txMgr : this.txManagers.values() ) {
            txMgr.destroy();
        }
        this.txManagers.clear();
    }
}
