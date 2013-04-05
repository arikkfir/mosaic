package org.mosaic.database.dao;

/**
 * @author arik
 */
public class KeyedUpdateResult extends UpdateResult
{
    private final Number generatedKey;

    public KeyedUpdateResult( int affectedRowsCount, Number generatedKey )
    {
        super( affectedRowsCount );
        this.generatedKey = generatedKey;
    }

    public Number getGeneratedKey()
    {
        return generatedKey;
    }

    @Override
    public String toString()
    {
        return "KeyedUpdateResult[key=" + this.generatedKey + ", affectedRows=" + getAffectedRowsCount() + "]";
    }
}
