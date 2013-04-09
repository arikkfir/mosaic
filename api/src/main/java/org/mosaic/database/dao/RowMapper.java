package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface RowMapper<T>
{
    T map( @Nonnull MapEx<String, Object> row );
}
