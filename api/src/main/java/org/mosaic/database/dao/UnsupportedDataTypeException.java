package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class UnsupportedDataTypeException extends DaoException
{
    @Nonnull
    private final Class<?> type;

    public UnsupportedDataTypeException( @Nonnull String message, @Nonnull Class<?> type )
    {
        super( message );
        this.type = type;
    }

    public UnsupportedDataTypeException( @Nonnull String message,
                                         @Nullable Throwable cause,
                                         @Nonnull Class<?> type )
    {
        super( message, cause );
        this.type = type;
    }

    @Nonnull
    public Class<?> getType()
    {
        return this.type;
    }
}
