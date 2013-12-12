package org.mosaic.datasource.impl;

import java.sql.Connection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.datasource.*;
import org.mosaic.modules.Service;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Service
public class TransactionManagerImpl implements TransactionManager
{
    private static final Logger LOG = LoggerFactory.getLogger( TransactionManagerImpl.class );

    @Nonnull
    private final ThreadLocal<TransactionImpl> transactionHolder = new ThreadLocal<>();

    @Nonnull
    @Override
    public TransactionImpl startTransaction( @Nonnull String name, boolean readOnly )
    {
        TransactionImpl tx = new TransactionImpl( name, getTransaction(), readOnly );
        this.transactionHolder.set( tx );
        return tx;
    }

    @Nullable
    @Override
    public TransactionImpl getTransaction()
    {
        return this.transactionHolder.get();
    }

    @Nonnull
    @Override
    public TransactionImpl requireTransaction()
    {
        TransactionImpl tx = getTransaction();
        if( tx == null )
        {
            throw new TransactionException( "no active transaction" );
        }
        return tx;
    }

    final class TransactionImpl implements Transaction
    {
        @Nonnull
        private final String name;

        @Nullable
        private final TransactionImpl parent;

        private final boolean readOnly;

        @Nonnull
        private final MapEx<String, Object> attributes;

        private TransactionImpl( @Nonnull String name, @Nullable TransactionImpl parent, boolean readOnly )
        {
            this.name = name;
            this.parent = parent;
            this.readOnly = readOnly;
            this.attributes = this.parent != null ? this.parent.attributes : new HashMapEx<String, Object>();
            if( this.parent != null && this.parent.readOnly && !this.readOnly )
            {
                throw new TransactionCreateException( "cannot create read-write transaction under read-only transactions" );
            }
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.name;
        }

        @Nullable
        @Override
        public Transaction getParent()
        {
            return this.parent;
        }

        @Override
        public boolean isReadOnly()
        {
            return this.readOnly;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getAttributes()
        {
            return this.attributes;
        }

        @Override
        public void commit()
        {
            if( this.parent == null )
            {
                if( this.readOnly )
                {
                    // read-only transactions are always rolled back, never committed
                    rollback();
                    return;
                }

                Connection connection = this.attributes.get( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( connection != null )
                {
                    try
                    {
                        connection.commit();
                    }
                    catch( Exception e )
                    {
                        try
                        {
                            rollback();
                        }
                        catch( Exception e1 )
                        {
                            LOG.error( "Could not rollback transaction '{}': {}", this.name, e1.getMessage(), e1 );
                        }
                        throw new TransactionCommitException( "could not commit '" + this.name + "' (attempted rollback): " + e.getMessage(), e );
                    }
                }
            }
        }

        @Override
        public void rollback()
        {
            if( this.parent == null )
            {
                Connection connection = this.attributes.get( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( connection != null )
                {
                    try
                    {
                        connection.rollback();
                    }
                    catch( Exception e )
                    {
                        // TODO: kill connection
                        throw new TransactionRollbackException( "could not rollback '" + this.name + "': " + e.getMessage(), e );
                    }
                }
            }
        }

        @Override
        public void close()
        {
            if( this.parent == null )
            {
                Connection connection = this.attributes.get( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( connection != null )
                {
                    try
                    {
                        connection.close();
                    }
                    catch( Exception e )
                    {
                        // TODO: kill connection
                        LOG.error( "Could not close transaction '{}': {}", e.getMessage(), e );
                    }
                }
            }
            TransactionManagerImpl.this.transactionHolder.set( this.parent );
        }
    }
}
