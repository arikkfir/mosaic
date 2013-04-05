package org.mosaic.database.dao;

/**
 * @author arik
 */
public class UpdateResult
{
    private final int affectedRowsCount;

    public UpdateResult( int affectedRowsCount )
    {
        this.affectedRowsCount = affectedRowsCount;
    }

    public int getAffectedRowsCount()
    {
        return affectedRowsCount;
    }

    @Override
    public String toString()
    {
        return "UpdateResult[affectedRows=" + this.affectedRowsCount + "]";
    }
}
