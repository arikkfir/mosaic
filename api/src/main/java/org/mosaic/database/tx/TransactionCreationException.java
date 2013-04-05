package org.mosaic.database.tx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class TransactionCreationException extends TransactionException
{
    public TransactionCreationException( @Nonnull String message )
    {
        super( message );
    }

    public TransactionCreationException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
