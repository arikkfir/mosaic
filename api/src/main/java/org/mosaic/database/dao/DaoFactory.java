package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

/**
 * @author arik
 */
public interface DaoFactory
{
    @Nonnull
    <T> T create( @Nonnull Class<T> type, @Nonnull DataSource dataSource );
}
