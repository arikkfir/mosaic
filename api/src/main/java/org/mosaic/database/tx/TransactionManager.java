package org.mosaic.database.tx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface TransactionManager
{
    @Nonnull
    Transaction begin( @Nonnull String name, boolean readOnly ) throws TransactionCreationException;

    @Nonnull
    Transaction getCurrentTransaction() throws NoTransactionException;

    Exception fail( @Nullable Exception exception ) throws Exception;

    void apply() throws Exception;

    interface TransactionListener
    {
        void onTransactionFailure( @Nonnull Transaction transaction, @Nullable Exception exception );

        void onTransactionSuccess( @Nonnull Transaction transaction ) throws Exception;

        void onTransactionCompletion( @Nonnull Transaction transaction, @Nullable Exception exception );
    }

    interface Transaction
    {
        @Nonnull
        String getName();

        boolean isRoot();

        boolean isReadOnly();

        boolean isFinished();

        void addTransactionListener( @Nonnull TransactionListener listener );
    }
}
