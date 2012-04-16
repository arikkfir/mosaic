package org.mosaic.server.db.impl;

import com.jolbox.bonecp.BoneCP;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import org.mosaic.transaction.ConnectionProxy;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * @author arik
 */
public class TransactionalDataSource extends BaseDataSource {

    private static final ThreadLocal<ConnectionHandler> HANDLERS = new ThreadLocal<>();

    private static final Class<?>[] CONN_PROXY_CLASSES = new Class<?>[] { ConnectionProxy.class };

    private final BoneCP pool;

    public TransactionalDataSource( BoneCP pool ) {
        super( pool.getConfig().getPoolName() );
        this.pool = pool;
    }

    public String getPoolName() {
        return this.pool.getConfig().getPoolName();
    }

    public void shutdown() {
        try {
            this.pool.shutdown();
        } catch( Exception e ) {
            this.logger.error( "Error shutting down connection pool '{}': {}", getPoolName(), e.getMessage(), e );
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        ConnectionHandler handlerForThread = HANDLERS.get();
        if( handlerForThread == null ) {
            handlerForThread = new ConnectionHandler();
            HANDLERS.set( handlerForThread );
        }
        return handlerForThread.getConnection();
    }

    @Override
    public Connection getConnection( String username, String password ) throws SQLException {
        throw new UnsupportedOperationException( "This data source is configured by Mosaic, and therefor does not allow ad-hoc username/password usage. Please use getConnection() and configure your username/password via Mosaic configuration facilities." );
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ( ( Long ) this.pool.getConfig().getConnectionTimeoutInMs() ).intValue() / 1000;
    }

    private class ConnectionHandler implements InvocationHandler {

        private final Connection proxyConnection;

        private volatile Connection pooledConnection;

        private volatile int useCount = 0;

        private ConnectionHandler() {
            ClassLoader proxyClassLoader = getClass().getClassLoader();
            this.proxyConnection = ( Connection ) newProxyInstance( proxyClassLoader, CONN_PROXY_CLASSES, this );
        }

        public Connection getConnection() throws SQLException {

            synchronized( this ) {

                if( ++this.useCount == 1 ) {
                    this.pooledConnection = pool.getConnection();
                }

            }
            return this.proxyConnection;

        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
            if( method.getName().equals( "equals" ) ) {

                // only proxies are equal
                return proxy == args[ 0 ];

            } else if( method.getName().equals( "hashCode" ) ) {

                // use the identity of the proxy as the hash code (hide the pooled connection)
                return System.identityHashCode( proxy );

            } else if( method.getName().equals( "unwrap" ) ) {

                // essentially 'proxy instanceof ((Class)args[0])'
                Class<?> targetType = ( Class<?> ) args[ 0 ];
                if( targetType.isInstance( proxy ) ) {
                    return proxy;
                }

            } else if( method.getName().equals( "isWrapperFor" ) ) {

                // essentially 'proxy instanceof ((Class)args[0])'
                Class<?> targetType = ( Class<?> ) args[ 0 ];
                if( targetType.isInstance( proxy ) ) {
                    return true;
                }

            } else if( method.getName().equals( "isClosed" ) ) {

                // as long as we have a pooled connection, we're golden; if no connection, we're closed
                return this.pooledConnection == null;

            } else if( method.getName().equals( "getTargetConnection" ) ) {

                // implementation of 'ConnectionProxy.getTargetConnection()' - return our pooled connection (might be null)
                return this.pooledConnection;

            } else if( method.getName().equals( "close" ) ) {

                // synchronize closing just in case someone passed the connection to another thread...
                synchronized( this ) {

                    if( this.useCount == 0 ) {

                        // already closed, log warning and exit
                        logger.warn( "JDBC connection closed more than times than acquired! (following exception is just for stack-tracing)", new RuntimeException() );
                        return null;

                    } else if( this.useCount == 1 ) {

                        // transaction entry point is closing - close connection and return
                        this.useCount = 0;

                        //TODO 4/16/12: commit or rollback
                        this.pooledConnection.commit();

                        this.pooledConnection.close();
                        this.pooledConnection = null;

                    } else {

                        // this is not the transaction entry point closing; just decrease the usage counter
                        this.useCount--;

                    }

                }
                return null;

            }

            // any other method just pass on to the pooled connection instance
            try {
                return method.invoke( this.pooledConnection, args );
            } catch( InvocationTargetException ex ) {
                throw ex.getCause();
            }
        }
    }

}
