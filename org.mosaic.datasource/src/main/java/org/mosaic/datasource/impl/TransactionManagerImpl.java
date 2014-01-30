package org.mosaic.datasource.impl;

import com.google.common.base.Optional;
import com.mchange.v2.c3p0.C3P0ProxyConnection;
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

    private static final Object[] EMPTY_OBJECTS_ARRAY = new Object[ 0 ];

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

                Optional<Connection> conHolder = this.attributes.find( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( conHolder.isPresent() )
                {
                    try
                    {
                        conHolder.get().commit();
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
                Optional<Connection> conHolder = this.attributes.find( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( conHolder.isPresent() )
                {
                    try
                    {
                        conHolder.get().rollback();
                    }
                    catch( Exception e )
                    {
                        killConnection( conHolder.get() );
                        throw new TransactionRollbackException( "could not rollback '" + this.name + "', connection possibly killed. Message was: " + e.getMessage(), e );
                    }
                }
            }
        }

        @Override
        public void close()
        {
            if( this.parent == null )
            {
                Optional<Connection> conHolder = this.attributes.find( ConfigurableDataSource.TX_CONNECTION_KEY, Connection.class );
                if( conHolder.isPresent() )
                {
                    try
                    {
                        conHolder.get().close();
                    }
                    catch( Exception e )
                    {
                        killConnection( conHolder.get() );
                        LOG.error( "Could not close transaction '{}', connection possibly killed. Message was: {}", e.getMessage(), e );
                    }
                }
            }
            TransactionManagerImpl.this.transactionHolder.set( this.parent );
        }

        private void killConnection( @Nonnull Connection connection )
        {
            try
            {
                C3P0ProxyConnection proxyConnection = ( C3P0ProxyConnection ) connection;
                proxyConnection.rawConnectionOperation( Connection.class.getMethod( "close" ),
                                                        C3P0ProxyConnection.RAW_CONNECTION,
                                                        EMPTY_OBJECTS_ARRAY );
            }
            catch( Exception killEx )
            {
                LOG.warn( "Error killing connection '{}' after failed release or rollback: {}", connection, killEx.getMessage(), killEx );
            }
        }
    }
}
