package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class TooManyRowsException extends DaoException
{
    public TooManyRowsException( @Nonnull String message )
    {
        super( message );
    }

    public TooManyRowsException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
