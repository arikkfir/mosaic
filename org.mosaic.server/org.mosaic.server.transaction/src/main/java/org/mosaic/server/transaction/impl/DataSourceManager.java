package org.mosaic.server.transaction.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.Home;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;

/**
 * @author arik
 */
@Component
public class DataSourceManager {

    private static final Logger LOG = LoggerFactory.getLogger( DataSourceManager.class );

    private static final long SCAN_INTERVAL = 1000;

    private final Map<Path, TransactionManagerImpl> txManagers = new HashMap<>();

    private JdbcDriverRegistrar jdbcDriverRegistrar;

    private ConversionService conversionService;

    private Home home;

    private Scanner scanner;

    @Autowired
    public void setJdbcDriverRegistrar( JdbcDriverRegistrar jdbcDriverRegistrar ) {
        this.jdbcDriverRegistrar = jdbcDriverRegistrar;
    }

    @Autowired
    public void setConversionService( ConversionService conversionService ) {
        this.conversionService = conversionService;
    }

    @ServiceRef
    public void setHome( Home home ) {
        this.home = home;
    }

    @PostConstruct
    public synchronized void init() {
        scan();

        this.scanner = new Scanner();
        Thread t = new Thread( scanner, "DataSourceWatcher" );
        t.setDaemon( true );
        t.start();
    }

    @PreDestroy
    public synchronized void destroy() throws SQLException {
        if( this.scanner != null ) {
            this.scanner.stop = true;
        }

        for( TransactionManagerImpl txMgr : this.txManagers.values() ) {
            txMgr.unregister();
        }
        this.txManagers.clear();
    }

    private synchronized void scan() {

        Path dataSourcesDir = this.home.getEtc().resolve( "data-sources" );
        if( Files.exists( dataSourcesDir ) && Files.isDirectory( dataSourcesDir ) ) {

            try( DirectoryStream<Path> stream = newDirectoryStream( dataSourcesDir, "*.properties" ) ) {

                for( Path configFile : stream ) {
                    TransactionManagerImpl txMgr = this.txManagers.get( configFile );
                    if( txMgr == null ) {
                        txMgr = new TransactionManagerImpl( configFile, this.jdbcDriverRegistrar, this.conversionService );
                        this.txManagers.put( configFile, txMgr );
                    }
                    txMgr.refresh();
                }

            } catch( IOException e ) {
                LOG.error( "Could not search for configurations in '{}': {}", this.home.getEtc(), e.getMessage(), e );
            }

        }

        this.txManagers.values().iterator();
        Iterator<TransactionManagerImpl> iterator = this.txManagers.values().iterator();
        while( iterator.hasNext() ) {
            TransactionManagerImpl txMgr = iterator.next();
            if( !exists( txMgr.getPath() ) ) {
                txMgr.unregister();
                iterator.remove();
            }
        }
    }

    private class Scanner implements Runnable {

        private boolean stop;

        @Override
        public void run() {
            while( !this.stop ) {
                try {
                    Thread.sleep( SCAN_INTERVAL );
                } catch( InterruptedException e ) {
                    break;
                }
                scan();
            }
        }
    }
}
