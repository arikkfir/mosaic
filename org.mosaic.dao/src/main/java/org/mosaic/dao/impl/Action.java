package org.mosaic.dao.impl;

import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.mosaic.dao.DaoException;
import org.mosaic.datasource.Transaction;
import org.mosaic.datasource.TransactionManager;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.method.MethodHandle;

import static org.mosaic.modules.Property.property;

/**
 * @author arik
 */
abstract class Action
{
    @Nonnull
    private final MethodHandle methodHandle;

    @Nonnull
    private final String dataSourceName;

    @Nonnull
    private final String transactionName;

    private final boolean readOnly;

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Service
    private TransactionManager transactionManager;

    protected Action( @Nonnull MethodHandle methodHandle, @Nonnull String dataSourceName, boolean readOnly )
    {
        this.methodHandle = methodHandle;
        this.dataSourceName = dataSourceName;
        this.transactionName = this.methodHandle.getName();
        this.readOnly = readOnly;
    }

    @Nullable
    final Object execute( @Nonnull Object... arguments ) throws SQLException
    {
        Transaction tx = this.transactionManager.startTransaction( this.transactionName, this.readOnly );
        try
        {
            ServiceReference<DataSource> reference = this.module.findService( DataSource.class, property( "name", this.dataSourceName ) );
            if( reference == null )
            {
                throw new DaoException( "data source '" + this.dataSourceName + "' not available" );
            }

            try( Connection connection = reference.service().get().getConnection() )
            {
                return invoke( connection, arguments );
            }
        }
        catch( Throwable e )
        {
            tx.rollback();
            throw e;
        }
        finally
        {
            tx.close();
        }
    }

    @Nonnull
    protected final MethodHandle getMethodHandle()
    {
        return this.methodHandle;
    }

    @Nullable
    protected abstract Object invoke( @Nonnull Connection connection, @Nonnull Object... arguments )
            throws SQLException;
}
