package org.mosaic.server.transaction.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import javax.sql.DataSource;
import org.mosaic.server.transaction.TransactionManager;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.READ;
import static org.mosaic.util.collection.Maps.mapFrom;
import static org.mosaic.util.logging.LoggerFactory.getBundleLogger;

/**
 * @author arik
 */
public class TransactionManagerImpl implements TransactionManager, DataSource
{
    public static final String[] TX_MGR_INTERFACES = new String[] {
            TransactionManager.class.getName(), DataSource.class.getName()
    };

    private final Logger logger;

    private final Path path;

    private final String name;

    private final BoneCPDataSourceWrapper rawDataSource;

    private TransactionAwareDataSourceProxy txDataSource;

    private DataSourceTransactionManager springTxMgr;

    private ServiceRegistration<?> registration;

    private long modificationTime;

    public TransactionManagerImpl( Path dataSourceFile, JdbcDriverRegistrar jdbcDriverRegistrar )
    {
        this.path = dataSourceFile;

        // discover name
        String fileName = this.path.getFileName().toString();
        this.name = fileName.substring( 0, fileName.length() - ".properties".length() );

        // create logger
        this.logger = getBundleLogger( TransactionManagerImpl.class, this.name );

        // create the actual BoneCP data source which will manage the connection pool
        this.rawDataSource = new BoneCPDataSourceWrapper( this.name, jdbcDriverRegistrar );

        // create a data-source proxy which will add spring-aware transactions to the data source
        this.txDataSource = new TransactionAwareDataSourceProxy( this.rawDataSource );

        // create a transaction manager used by @Transactional classes
        this.springTxMgr = new DataSourceTransactionManager( this.rawDataSource );
        this.springTxMgr.setValidateExistingTransaction( true );
        this.springTxMgr.afterPropertiesSet();
    }

    public Path getPath()
    {
        return path;
    }

    public void refresh()
    {
        if( isDirectory( this.path ) || !exists( this.path ) || !isReadable( this.path ) )
        {
            if( this.modificationTime > 0 )
            {
                logger.warn( "Data source '{}' no longer exists/readable at: {}", this.name, this.path );
                this.modificationTime = 0;
                unregister();
            }
        }
        else
        {
            try
            {
                long modificationTime = getLastModifiedTime( this.path ).toMillis();
                if( modificationTime > this.modificationTime )
                {
                    this.modificationTime = modificationTime;

                    logger.info( "Creating data source '{}' from: {}", this.name, this.path );
                    Properties properties = new Properties();
                    try( InputStream inputStream = newInputStream( this.path, READ ) )
                    {
                        properties.load( inputStream );
                    }
                    register( new MapAccessor<>( mapFrom( properties ) ) );
                }
            }
            catch( IOException e )
            {
                logger.error( "Could not refresh data source '{}': {}", this.path.getFileName().toString(), e.getMessage(), e );
            }
        }
    }

    public void register( MapAccessor<String, String> c )
    {
        this.rawDataSource.init( c );

        // create a transaction manager for the *RAW* data source (NEVER TO THE TX DATA SOURCE! THE TX-MGR MUST WORK AGAINST THE ACTUAL DATA SOURCE!)
        this.springTxMgr.setNestedTransactionAllowed( c.get( "nestedTransactionsAllowed", Boolean.class, false ) );
        this.springTxMgr.setRollbackOnCommitFailure( c.get( "rollbackOnCommitFailure", Boolean.class, false ) );

        // register as a data source and transaction manager
        Dictionary<String, Object> dsDict = new Hashtable<>();
        dsDict.put( "name", this.name );
        if( this.registration != null )
        {
            this.registration.setProperties( dsDict );
        }
        else
        {
            Bundle bundle = FrameworkUtil.getBundle( getClass() );
            this.registration = bundle.getBundleContext().registerService( TX_MGR_INTERFACES, this, dsDict );
        }
    }

    public void unregister()
    {
        this.logger.info( "Shutting down connection pool '{}'", this.rawDataSource.getPoolName() );
        try
        {
            this.registration.unregister();
        }
        catch( IllegalStateException ignore )
        {
        }

        try
        {
            this.rawDataSource.close();
        }
        catch( Exception e )
        {
            this.logger.error( "Could not close data source '{}': {}", this.rawDataSource.getPoolName(), e.getMessage(), e );
        }
    }

    @Override
    public Object begin( String name )
    {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setName( name );
        txDef.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRED );
        return this.springTxMgr.getTransaction( txDef );
    }

    @Override
    public void rollback( Object tx )
    {
        if( tx instanceof TransactionStatus )
        {
            this.springTxMgr.rollback( ( TransactionStatus ) tx );
        }
        else
        {
            throw new IllegalArgumentException( tx + " is not a transaction object" );
        }
    }

    @Override
    public void commit( Object tx )
    {
        if( tx instanceof TransactionStatus )
        {
            this.springTxMgr.commit( ( TransactionStatus ) tx );
        }
        else
        {
            throw new IllegalArgumentException( tx + " is not a transaction object" );
        }
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return this.txDataSource.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException
    {
        return this.txDataSource.getConnection( username, password );
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return this.txDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter( PrintWriter out ) throws SQLException
    {
        this.txDataSource.setLogWriter( out );
    }

    @Override
    public void setLoginTimeout( int seconds ) throws SQLException
    {
        this.txDataSource.setLoginTimeout( seconds );
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return this.txDataSource.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return this.txDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap( Class<T> type ) throws SQLException
    {
        return this.txDataSource.unwrap( type );
    }

    @Override
    public boolean isWrapperFor( Class<?> type ) throws SQLException
    {
        return this.txDataSource.isWrapperFor( type );
    }
}
