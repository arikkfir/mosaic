package org.mosaic.database.tx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class TransactionCommitException extends TransactionException
{
    @Nonnull
    private final TransactionManager.Transaction transaction;

    public TransactionCommitException( @Nonnull String message,
                                       @Nonnull TransactionManager.Transaction transaction )
    {
        super( message );
        this.transaction = transaction;
    }

    public TransactionCommitException( @Nonnull String message,
                                       @Nullable Throwable cause,
                                       @Nonnull TransactionManager.Transaction transaction )
    {
        super( message, cause );
        this.transaction = transaction;
    }

    @Nonnull
    public TransactionManager.Transaction getTransaction()
    {
        return transaction;
    }
}
