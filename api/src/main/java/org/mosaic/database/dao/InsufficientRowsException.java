package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class InsufficientRowsException extends DaoException
{
    public InsufficientRowsException( @Nonnull String message )
    {
        super( message );
    }

    public InsufficientRowsException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
