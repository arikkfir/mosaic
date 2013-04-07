package org.mosaic.database.tx.impl;

import java.sql.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.util.collect.MapEx;

import static org.mosaic.database.tx.impl.TransactionManagerImpl.TX_MARKER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author arik
 */
@Bean
public class DataSourceFactory
{
    private static final org.slf4j.Logger LOG = getLogger( DataSourceFactory.class );

    @Nonnull
    private TransactionManagerImpl transactionManager;

    @BeanRef
    public void setTransactionManager( @Nonnull TransactionManagerImpl transactionManager )
    {
        this.transactionManager = transactionManager;
    }

    public ConfigurableDataSource createDataSource( @Nonnull String name,
                                                    @Nonnull JdbcDriverFinder jdbcDriverFinder,
                                                    @Nonnull MapEx<String, String> cfg )
    {
        try
        {
            try
            {
                ConfigurableDataSource pool = new ConfigurableDataSource( name, jdbcDriverFinder, cfg );
                Connection connection = pool.getConnection();
                connection.close();
                return pool;
            }
            catch( @SuppressWarnings( "deprecation" ) org.apache.commons.dbcp.SQLNestedException e )
            {
                Throwable cause = e.getCause();
                if( cause instanceof SQLException )
                {
                    throw ( SQLException ) cause;
                }
                else
                {
                    throw e;
                }
            }
        }
        catch( SQLDriverNotFoundException e )
        {
            LOG.warn( "Could not find JDBC driver for data source '{}': {}", name, e.getMessage(), e );
        }
        catch( Exception e )
        {
            LOG.error( "Could not create new data source '{}': {}", name, e.getMessage(), e );
        }
        return null;
    }

    public class ConfigurableDataSource extends BasicDataSource
            implements ConnectionFactory, TransactionManager.TransactionListener
    {
        @Nonnull
        private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

        @Nonnull
        private final String name;

        @Nonnull
        private final Driver driver;

        public ConfigurableDataSource( @Nonnull String name,
                                       @Nonnull JdbcDriverFinder jdbcDriverFinder,
                                       @Nonnull MapEx<String, String> cfg )
                throws SQLDriverNotFoundException
        {
            this.name = name;

            // configure
            setAccessToUnderlyingConnectionAllowed( cfg.get( "accessToUnderlyingConnectionAllowed", Boolean.class, false ) );
            setConnectionProperties( cfg.get( "properties", String.class, "" ) );
            setInitialSize( cfg.get( "initialSize", Integer.class, 1 ) );
            setLogAbandoned( cfg.get( "logAbandoned", Boolean.class, true ) );
            setMaxActive( cfg.get( "maxActive", Integer.class, 50 ) );
            setMaxIdle( cfg.get( "maxIdle", Integer.class, 3 ) );
            setMaxOpenPreparedStatements( cfg.get( "maxOpenPreparedStatements", Integer.class, 150 ) );
            setMaxWait( cfg.get( "maxWait", Long.class, 1000l * 20 ) );
            setMinEvictableIdleTimeMillis( cfg.get( "minEvictableIdleTimeMillis", Long.class, 1000l * 60 * 3 ) );
            setMinIdle( cfg.get( "minIdle", Integer.class, 0 ) );
            setNumTestsPerEvictionRun( cfg.get( "numTestsPerEvictionRun", Integer.class, 3 ) );
            setPassword( cfg.get( "password", String.class ) );
            setPoolPreparedStatements( cfg.get( "poolPreparedStatements", Boolean.class, true ) );
            setRemoveAbandoned( cfg.get( "removeAbandoned", Boolean.class, true ) );
            setRemoveAbandonedTimeout( cfg.get( "removeAbandonedTimeout", Integer.class, 300 ) );
            setTestOnBorrow( cfg.get( "testOnBorrow", Boolean.class, true ) );
            setTestOnReturn( cfg.get( "testOnReturn", Boolean.class, false ) );
            setTestWhileIdle( cfg.get( "testWhileIdle", Boolean.class, true ) );
            setTimeBetweenEvictionRunsMillis( cfg.get( "timeBetweenEvictionRunsMillis", Long.class, 1000l * 60 ) );
            setUrl( createJdbcUrl( cfg ) );
            setUsername( cfg.get( "username", String.class ) );
            setValidationQuery( cfg.get( "validationQuery", String.class, "SELECT 1" ) );
            setValidationQueryTimeout( cfg.get( "validationQueryTimeout", Integer.class, 15 ) );

            // find our JDBC driver
            Driver driver = jdbcDriverFinder.getDriver( this.url );
            if( driver == null )
            {
                throw new SQLDriverNotFoundException( "No JDBC driver found for creating data source" );
            }
            else
            {
                this.driver = driver;
            }
        }

        @Nonnull
        public Driver getDriver()
        {
            return driver;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public Connection createConnection() throws SQLException
        {
            return this.driver.connect( this.url, this.connectionProperties );
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            Connection connection = this.connectionHolder.get();
            if( connection == null )
            {
                TransactionManagerImpl.TransactionImpl tx = transactionManager.getCurrentTransaction();
                getLogger( tx.getName() ).debug(
                        TX_MARKER,
                        "Creating connection from data source '{}' for transaction '{}'",
                        this.name,
                        tx.getName()
                );

                // create & initialize the connection
                connection = super.getConnection();
                connection.prepareCall( "START TRANSACTION" ).execute();
                connection.setAutoCommit( false );
                connection.setReadOnly( tx.isReadOnly() );
                connection.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
                connection.setHoldability( ResultSet.CLOSE_CURSORS_AT_COMMIT );
                connection.clearWarnings();

                // save connection in the connection holder so we reuse the same connection over the couse of the tx
                this.connectionHolder.set( connection );

                // request notifications when the transaction finishes
                tx.addTransactionListener( this );
            }
            return new NoCloseConnectionWrapper( connection );
        }

        @Override
        public void onTransactionFailure( @Nonnull TransactionManager.Transaction transaction,
                                          @Nullable Exception exception )
        {
            Connection connection = this.connectionHolder.get();
            if( connection != null )
            {
                rollback( transaction, connection );
            }
        }

        @Override
        public void onTransactionSuccess( @Nonnull TransactionManager.Transaction transaction ) throws Exception
        {
            Connection connection = this.connectionHolder.get();
            if( connection != null )
            {
                commit( transaction, connection );
            }
        }

        @Override
        public void onTransactionCompletion( @Nonnull TransactionManager.Transaction transaction,
                                             @Nullable Exception exception )
        {
            Connection connection = this.connectionHolder.get();
            if( connection != null )
            {
                close( transaction, connection );
            }
        }

        private void commit( TransactionManager.Transaction transaction, Connection connection ) throws Exception
        {
            boolean readOnly = connection.isReadOnly();
            if( readOnly )
            {
                getLogger( transaction.getName() ).debug(
                        TX_MARKER,
                        "Connection from data source '{}' in transaction '{}' is read-only - rolling back instead of committiong",
                        this.name,
                        transaction.getName()
                );
                rollback( transaction, connection );
            }
            else
            {
                try
                {
                    getLogger( transaction.getName() ).debug(
                            TX_MARKER,
                            "Committing connection from data source '{}' for transaction '{}'",
                            this.name,
                            transaction.getName()
                    );
                    connection.commit();
                }
                catch( Exception e )
                {
                    getLogger( transaction.getName() ).debug(
                            TX_MARKER,
                            "Error committing connection from data source '{}' for transaction '{}' - rolling back",
                            this.name,
                            transaction.getName(),
                            e
                    );
                    throw e;
                }
            }
        }

        @SuppressWarnings( "unchecked" )
        private void rollback( @Nonnull TransactionManager.Transaction transaction, @Nonnull Connection connection )
        {
            try
            {
                getLogger( transaction.getName() ).debug(
                        TX_MARKER,
                        "Rolling back connection from data source '{}' for transaction '{}'",
                        this.name,
                        transaction.getName()
                );
                connection.rollback();
            }
            catch( Exception e )
            {
                getLogger( transaction.getName() ).warn(
                        TX_MARKER,
                        "Error rolling back connection from data source '{}' in transaction '{}', connection will be closed (exception below is what failed the rollback)",
                        this.name,
                        transaction.getName(),
                        e
                );
                kill( transaction, connection );
            }
        }

        private void close( TransactionManager.Transaction transaction, Connection connection )
        {
            org.slf4j.Logger logger = getLogger( transaction.getName() );
            logger.debug(
                    TX_MARKER,
                    "Returning connection from data source '{}' for transaction '{}' to the connection pool (transaction completed)",
                    this.name,
                    transaction.getName()
            );

            try
            {
                connection.prepareCall( "UNLOCK TABLES" ).execute();
                connection.close();
            }
            catch( Exception e )
            {
                logger.debug(
                        TX_MARKER,
                        "Error returning connection from data source '{}' for transaction '{}' to the connection pool, closing it",
                        this.name,
                        transaction.getName(),
                        e
                );
                kill( transaction, connection );
            }
            finally
            {
                this.connectionHolder.remove();
            }
        }

        @SuppressWarnings( "unchecked" )
        private void kill( TransactionManager.Transaction transaction, Connection connection )
        {
            if( this.connectionPool != null && !this.connectionPool.isClosed() )
            {
                try
                {
                    connectionPool.invalidateObject( connection );
                }
                catch( Exception e1 )
                {
                    getLogger( transaction.getName() ).warn(
                            TX_MARKER,
                            "Error killing connection from data source '{}' in transaction '{}' (exception below is what failed killing connection)",
                            this.name,
                            transaction.getName(),
                            e1
                    );
                }
            }
        }

        @Override
        protected ConnectionFactory createConnectionFactory() throws SQLException
        {
            // Can't test without a validationQuery
            if( this.validationQuery == null )
            {
                setTestOnBorrow( false );
                setTestOnReturn( false );
                setTestWhileIdle( false );
            }

            // Set up the driver connection factory we will use
            String user = this.username;
            if( user != null )
            {
                this.connectionProperties.put( "user", user );
            }
            else
            {
                getLogger( getClass() ).debug(
                        TX_MARKER,
                        "Creating a data-source connection factory for data source '{}' with no username",
                        this.name
                );
            }

            String pwd = this.password;
            if( pwd != null )
            {
                this.connectionProperties.put( "password", pwd );
            }
            else
            {
                getLogger( getClass() ).debug(
                        TX_MARKER,
                        "Creating a data-source connection factory for data source '{}' with no password",
                        this.name
                );
            }
            return this;
        }

        private String createJdbcUrl( MapEx<String, String> data )
        {
            final StringBuilder url = new StringBuilder( 100 );
            url.append( data.require( "url", String.class ) );

            final AtomicReference<Character> delimiter = new AtomicReference<>();

            for( String key : data.keySet() )
            {
                if( key.startsWith( "connection." ) )
                {
                    String propertyName = key.substring( "connection.".length() );
                    String value = data.require( key, String.class );

                    if( delimiter.get() == null )
                    {
                        url.append( '?' );
                        delimiter.set( '&' );
                    }
                    else
                    {
                        url.append( '&' );
                    }

                    url.append( propertyName ).append( '=' ).append( value );
                }
            }

            return url.toString();
        }
    }
}
