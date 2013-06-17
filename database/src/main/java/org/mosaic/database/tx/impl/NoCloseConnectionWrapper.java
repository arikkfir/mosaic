package org.mosaic.database.tx.impl;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class NoCloseConnectionWrapper implements Connection
{
    @Nonnull
    private final Connection target;

    public NoCloseConnectionWrapper( @Nonnull Connection target )
    {
        this.target = target;
    }

    @Override
    @Nonnull
    public Statement createStatement() throws SQLException
    {
        return target.createStatement();
    }

    @Override
    @Nonnull
    public PreparedStatement prepareStatement( String sql ) throws SQLException
    {
        return target.prepareStatement( sql );
    }

    @Override
    @Nonnull
    public CallableStatement prepareCall( String sql ) throws SQLException
    {
        return target.prepareCall( sql );
    }

    @Override
    @Nonnull
    public String nativeSQL( String sql ) throws SQLException
    {
        return target.nativeSQL( sql );
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return target.getAutoCommit();
    }

    @Override
    public void setAutoCommit( boolean autoCommit ) throws SQLException
    {
        // DOES NOT CALL target.setAutoCommit() TO PREVENT CLIENT CODE FROM CHANGING OUR TRANSACTION SEMANTICS
    }

    @Override
    public void commit() throws SQLException
    {
        // DOES NOT CALL target.commit() TO PREVENT CLIENT CODE FROM COMMITTING IN THE MIDDLE OF A TRANSACTION - ONLY
        // TRANSACTION FRAMEWORK CAN COMMIT THE CONNECTION
    }

    @Override
    public void rollback() throws SQLException
    {
        // allow rollback - we don't care if the client code wants to revert its changes although its not the smartest thing to do - they should just abort the tx
        target.rollback();
    }

    @Override
    public void close() throws SQLException
    {
        // DOES NOT CALL target.close() TO PREVENT CLIENT CODE FROM CLOSING OUR CONNECTION - ONLY TRANSACTION FRAMEWORK
        // CAN CLOSE THE CONNECTION (which is actually returning the connection to the pool, not really closing it)
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return target.isClosed();
    }

    @Override
    @Nonnull
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return target.getMetaData();
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return target.isReadOnly();
    }

    @Override
    public void setReadOnly( boolean readOnly ) throws SQLException
    {
        // DOES NOT CALL target.setReadOnly() TO PREVENT CLIENT CODE FROM CHANGING OUR TRANSACTION SEMANTICS
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return target.getCatalog();
    }

    @Override
    public void setCatalog( String catalog ) throws SQLException
    {
        target.setCatalog( catalog );
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return target.getTransactionIsolation();
    }

    @Override
    public void setTransactionIsolation( int level ) throws SQLException
    {
        // DOES NOT CALL target.setTransactionIsolation() TO PREVENT CLIENT CODE FROM CHANGING OUR TRANSACTION SEMANTICS
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return target.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        target.clearWarnings();
    }

    @Override
    public Statement createStatement( int resultSetType, int resultSetConcurrency ) throws SQLException
    {
        return target.createStatement( resultSetType, resultSetConcurrency );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency )
            throws SQLException
    {
        return target.prepareStatement( sql, resultSetType, resultSetConcurrency );
    }

    @Override
    public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException
    {
        return target.prepareCall( sql, resultSetType, resultSetConcurrency );
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return target.getTypeMap();
    }

    @Override
    public void setTypeMap( Map<String, Class<?>> map ) throws SQLException
    {
        target.setTypeMap( map );
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return target.getHoldability();
    }

    @Override
    public void setHoldability( int holdability ) throws SQLException
    {
        // DOES NOT CALL target.setHoldability() TO PREVENT CLIENT CODE FROM CHANGING OUR TRANSACTION SEMANTICS
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Savepoint setSavepoint( String name ) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback( Savepoint savepoint ) throws SQLException
    {
        target.rollback( savepoint );
    }

    @Override
    public void releaseSavepoint( Savepoint savepoint ) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Statement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability )
            throws SQLException
    {
        return target.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability );
    }

    @Override
    public PreparedStatement prepareStatement( String sql,
                                               int resultSetType,
                                               int resultSetConcurrency,
                                               int resultSetHoldability ) throws SQLException
    {
        return target.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
    }

    @Override
    public CallableStatement prepareCall( String sql,
                                          int resultSetType,
                                          int resultSetConcurrency,
                                          int resultSetHoldability ) throws SQLException
    {
        return target.prepareCall( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws SQLException
    {
        return target.prepareStatement( sql, autoGeneratedKeys );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, int[] columnIndexes ) throws SQLException
    {
        return target.prepareStatement( sql, columnIndexes );
    }

    @Override
    public PreparedStatement prepareStatement( String sql, String[] columnNames ) throws SQLException
    {
        return target.prepareStatement( sql, columnNames );
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return target.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return target.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return target.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return target.createSQLXML();
    }

    @Override
    public boolean isValid( int timeout ) throws SQLException
    {
        return target.isValid( timeout );
    }

    @Override
    public void setClientInfo( String name, String value ) throws SQLClientInfoException
    {
        target.setClientInfo( name, value );
    }

    @Override
    public String getClientInfo( String name ) throws SQLException
    {
        return target.getClientInfo( name );
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return target.getClientInfo();
    }

    @Override
    public void setClientInfo( Properties properties ) throws SQLClientInfoException
    {
        target.setClientInfo( properties );
    }

    @Override
    public Array createArrayOf( String typeName, Object[] elements ) throws SQLException
    {
        return target.createArrayOf( typeName, elements );
    }

    @Override
    public Struct createStruct( String typeName, Object[] attributes ) throws SQLException
    {
        return target.createStruct( typeName, attributes );
    }

    @Override
    public String getSchema() throws SQLException
    {
        return target.getSchema();
    }

    @Override
    public void setSchema( String schema ) throws SQLException
    {
        target.setSchema( schema );
    }

    @Override
    public void abort( Executor executor ) throws SQLException
    {
        target.abort( executor );
    }

    @Override
    public void setNetworkTimeout( Executor executor, int milliseconds ) throws SQLException
    {
        target.setNetworkTimeout( executor, milliseconds );
    }

    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return target.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException
    {
        return target.unwrap( iface );
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) throws SQLException
    {
        return target.isWrapperFor( iface );
    }
}
