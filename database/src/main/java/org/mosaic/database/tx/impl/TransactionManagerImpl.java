package org.mosaic.database.tx.impl;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.database.tx.NoTransactionException;
import org.mosaic.database.tx.TransactionCommitException;
import org.mosaic.database.tx.TransactionCreationException;
import org.mosaic.database.tx.TransactionManager;
import org.mosaic.lifecycle.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * @author arik
 */
@Service(TransactionManager.class)
public class TransactionManagerImpl implements TransactionManager
{
    public static final Marker TX_MARKER = MarkerFactory.getMarker( "appender:tx" );

    private final ThreadLocal<TransactionImpl> transactionHolder = new ThreadLocal<>();

    @Nonnull
    @Override
    public Transaction begin( @Nonnull String name, boolean readOnly ) throws TransactionCreationException
    {
        return new TransactionImpl( name, readOnly );
    }

    @Nonnull
    @Override
    public TransactionImpl getCurrentTransaction() throws NoTransactionException
    {
        TransactionImpl tx = this.transactionHolder.get();
        if( tx == null )
        {
            throw new NoTransactionException();
        }
        else
        {
            return tx;
        }
    }

    @Override
    public void fail( @Nullable Exception exception ) throws NoTransactionException
    {
        getCurrentTransaction().fail( exception );
    }

    @Override
    public void apply() throws TransactionCommitException
    {
        getCurrentTransaction().apply();
    }

    public class TransactionImpl implements Transaction
    {
        @Nonnull
        private final String name;

        private final TransactionImpl parent;

        private final boolean readOnly;

        @Nullable
        private List<TransactionListener> listeners;

        public TransactionImpl( @Nonnull String name, boolean readOnly )
        {
            this.name = name;
            this.readOnly = readOnly;
            this.parent = transactionHolder.get();
            if( this.parent != null && this.parent.readOnly && !this.readOnly )
            {
                throw new TransactionCreationException( "Cannot create read-write transaction nested in a read-only transaction (" + this.name + " inside " + this.parent.name + ")" );
            }
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public boolean isRoot()
        {
            return this.parent == null;
        }

        @Override
        public boolean isReadOnly()
        {
            return this.readOnly;
        }

        @Override
        public boolean isFinished()
        {
            return transactionHolder.get() == this;
        }

        @Override
        public void addTransactionListener( @Nonnull TransactionListener listener )
        {
            if( this.parent == null )
            {
                if( this.listeners == null )
                {
                    this.listeners = new LinkedList<>();
                }
                this.listeners.add( listener );
            }
            else
            {
                this.parent.addTransactionListener( listener );
            }
        }

        public void fail( @Nullable Exception exception )
        {
            Logger logger = LoggerFactory.getLogger( this.name );
            logger.debug( "Rolling back transaction '{}'", this.name );

            // ignore TransactionCommitException - the 'apply' method already called 'fail' when that happened
            if( !TransactionCommitException.class.isInstance( exception ) )
            {
                if( this.parent == null )
                {
                    if( this.listeners != null )
                    {
                        for( TransactionListener listener : this.listeners )
                        {
                            try
                            {
                                listener.onTransactionFailure( this, exception );
                            }
                            catch( Exception e )
                            {
                                logger.warn(
                                        "Transaction listener '{}' threw an exception while rolling back transaction '{}': {}",
                                        listener,
                                        this.name,
                                        e.getMessage(),
                                        e
                                );
                            }
                        }
                    }
                    notifyCompletion( exception );
                    transactionHolder.remove();
                }
                else
                {
                    transactionHolder.set( this.parent );
                }
            }
        }

        public void apply() throws TransactionCommitException
        {
            Logger logger = LoggerFactory.getLogger( this.name );
            logger.debug( "Applying transaction '{}'", this.name );

            if( this.parent == null )
            {
                TransactionCommitException exception = null;

                if( this.listeners != null )
                {
                    for( TransactionListener listener : this.listeners )
                    {
                        try
                        {
                            listener.onTransactionSuccess( this );
                        }
                        catch( Exception e )
                        {
                            if( exception == null )
                            {
                                exception = new TransactionCommitException( "Error applying (committing) transaction '" + this.name + "'", this );
                                exception.addSuppressed( e );
                            }
                        }
                    }

                }

                if( exception != null )
                {
                    // transaction could not applied - one of the listeners threw an exception, so fail instead
                    // the 'fail' method will remove us from the transactionHolder and notify completion for us
                    fail( exception );
                    throw exception;
                }
                else
                {
                    // all is well - notify completion
                    notifyCompletion( null );
                }
                transactionHolder.remove();
            }
        }

        private void notifyCompletion( @Nullable Exception exception )
        {
            Logger logger = LoggerFactory.getLogger( this.name );
            if( this.listeners != null )
            {
                for( TransactionListener listener : this.listeners )
                {
                    try
                    {
                        listener.onTransactionCompletion( this, exception );
                    }
                    catch( Exception e )
                    {
                        logger.warn(
                                "Transaction listener '{}' threw an exception in completion hook of transaction '{}': {}",
                                listener,
                                this.name,
                                e.getMessage(),
                                e
                        );
                    }
                }
            }
        }
    }
}
