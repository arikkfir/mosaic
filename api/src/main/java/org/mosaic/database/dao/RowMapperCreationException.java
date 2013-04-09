package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class RowMapperCreationException extends DaoException
{
    public RowMapperCreationException( @Nonnull String message )
    {
        super( message );
    }

    public RowMapperCreationException( @Nonnull String message, @Nullable Throwable cause )
    {
        super( message, cause );
    }
}
