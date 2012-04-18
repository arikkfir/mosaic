package org.mosaic.server.transaction.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.sql.DataSource;
import org.mosaic.config.Configuration;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.transaction.TransactionManager;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author arik
 */
public class TransactionManagerImpl implements TransactionManager, DataSource {

    private static final Logger LOG = LoggerFactory.getBundleLogger( TransactionManagerImpl.class );

    private final BoneCPDataSourceWrapper rawDataSource;

    private TransactionAwareDataSourceProxy txDataSource;

    private DataSourceTransactionManager springTxMgr;

    private ServiceRegistration registration;

    public TransactionManagerImpl( String name, JdbcDriverRegistrar jdbcDriverRegistrar ) {

        // create the actual BoneCP data source which will manage the connection pool
        this.rawDataSource = new BoneCPDataSourceWrapper( name, jdbcDriverRegistrar );

        // create a data-source proxy which will add spring-aware transactions to the data source
        this.txDataSource = new TransactionAwareDataSourceProxy( this.rawDataSource );

        // create a transaction manager used by @Transactional classes
        this.springTxMgr = new DataSourceTransactionManager( this.rawDataSource );
        this.springTxMgr.setValidateExistingTransaction( true );
        this.springTxMgr.afterPropertiesSet();
    }

    public void updateConfiguration( Configuration configuration ) {
        this.rawDataSource.init( configuration );

        // create a transaction manager for the *RAW* data source (NEVER TO THE TX DATA SOURCE! THE TX-MGR MUST WORK AGAINST THE ACTUAL DATA SOURCE!)
        this.springTxMgr.setNestedTransactionAllowed( configuration.get( "nestedTransactionsAllowed", Boolean.class, false ) );
        this.springTxMgr.setRollbackOnCommitFailure( configuration.get( "rollbackOnCommitFailure", Boolean.class, false ) );

        // register as a data source and transaction manager
        Dictionary<String, Object> dsDict = new Hashtable<>();
        dsDict.put( "name", configuration.getName() );
        this.registration = FrameworkUtil.getBundle( getClass() ).getBundleContext().registerService(
                new String[] {
                        TransactionManager.class.getName(),
                        DataSource.class.getName(),
                        PlatformTransactionManager.class.getName()
                },
                this, dsDict );
    }

    public void destroy() {
        LOG.info( "Shutting down connection pool '{}'", this.rawDataSource.getPoolName() );
        try {
            this.registration.unregister();
        } catch( IllegalStateException ignore ) {
        }
        try {
            this.rawDataSource.close();
        } catch( Exception e ) {
            LOG.error( "Could not close data source '{}': {}", this.rawDataSource.getPoolName(), e.getMessage(), e );
        }
    }

    @Override
    public Object begin( String name ) {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setName( name );
        txDef.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRED );
        return this.springTxMgr.getTransaction( txDef );
    }

    @Override
    public void rollback( Object tx ) {
        if( tx instanceof TransactionStatus ) {
            this.springTxMgr.rollback( ( TransactionStatus ) tx );
        } else {
            throw new IllegalArgumentException( tx + " is not a transaction object" );
        }
    }

    @Override
    public void commit( Object tx ) {
        if( tx instanceof TransactionStatus ) {
            this.springTxMgr.commit( ( TransactionStatus ) tx );
        } else {
            throw new IllegalArgumentException( tx + " is not a transaction object" );
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.txDataSource.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException {
        return this.txDataSource.getConnection( username, password );
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.txDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException {
        this.txDataSource.setLogWriter( out );
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException {
        this.txDataSource.setLoginTimeout( seconds );
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.txDataSource.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.txDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap( Class<T> type ) throws SQLException {
        return this.txDataSource.unwrap( type );
    }

    @Override
    public boolean isWrapperFor( Class<?> type ) throws SQLException {
        return this.txDataSource.isWrapperFor( type );
    }
}
