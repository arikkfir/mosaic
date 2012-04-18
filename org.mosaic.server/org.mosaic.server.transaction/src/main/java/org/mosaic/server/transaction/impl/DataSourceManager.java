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

    private final Map<String, TransactionManagerImpl> pools = new HashMap<>();

    private JdbcDriverRegistrar jdbcDriverRegistrar;

    @Autowired
    public void setJdbcDriverRegistrar( JdbcDriverRegistrar jdbcDriverRegistrar ) {
        this.jdbcDriverRegistrar = jdbcDriverRegistrar;
    }

    @ServiceBind( filter = DATASOURCE_FILTER )
    public synchronized void addDataSourceConfiguration( Configuration configuration ) throws SQLException {
        //TODO 4/18/12: support driver selection in ds configuration (wait until driver is loaded, etc, like tracker)
        TransactionManagerImpl transactionSource = this.pools.get( configuration.getName() );
        if( transactionSource == null ) {
            transactionSource = new TransactionManagerImpl( configuration.getName(), this.jdbcDriverRegistrar );
            this.pools.put( configuration.getName(), transactionSource );
        }
        transactionSource.updateConfiguration( configuration );
    }

    @ServiceUnbind( filter = DATASOURCE_FILTER )
    public synchronized void removeDataSourceConfiguration( Configuration configuration ) {
        TransactionManagerImpl removed = this.pools.remove( configuration.getName() );
        if( removed != null ) {
            removed.destroy();
        }
    }

    @PreDestroy
    public synchronized void destroy() throws SQLException {
        for( TransactionManagerImpl transactionSource : this.pools.values() ) {
            transactionSource.destroy();
        }
        this.pools.clear();
    }
}
