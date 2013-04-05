package org.mosaic.database.tx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public abstract class TransactionException extends RuntimeException
{
    protected TransactionException( @Nonnull String message )
    {
        super( message );
    }

    protected TransactionException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
