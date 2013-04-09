package org.mosaic.database.dao;

import java.sql.SQLException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface Batch
{
    @Nonnull
    Batch set( @Nonnull String name, @Nullable Object value );

    @Nonnull
    Batch next();

    @Nonnull
    Batch add( @Nonnull Map<String, Object> parameters );

    @Nonnull
    BatchUpdateResult execute() throws SQLException;
}
