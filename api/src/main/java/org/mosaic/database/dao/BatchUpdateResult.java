package org.mosaic.database.dao;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface BatchUpdateResult
{
    int getAffectedRowsCount();

    @Nonnull
    List<Integer> getAffectedRowsCountByBatch();
}
