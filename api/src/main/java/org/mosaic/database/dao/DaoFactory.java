package org.mosaic.database.dao;

/**
 * @author arik
 */
public interface DaoFactory
{
    <T> T create( Class<T> type );

    <T> T create( Class<T> type, String secondaryDataSourceNAme );
}
