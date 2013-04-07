package org.mosaic.database.dao;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface RowCallback
{
    void process( @Nonnull MapEx<String, Object> row );
}
