package org.mosaic.database.dao;

/**
 * @author arik
 */
public interface UpdateResult
{
    int getAffectedRowsCount();

    Number getGeneratedKey();
}
