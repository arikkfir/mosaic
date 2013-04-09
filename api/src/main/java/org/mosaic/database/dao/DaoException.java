package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class DaoException extends RuntimeException
{
    public DaoException( @Nonnull String message )
    {
        super( message );
    }

    public DaoException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
