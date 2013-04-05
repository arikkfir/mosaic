package org.mosaic.database.dao;

import java.util.Map;

/**
 * @author arik
 */
public interface Batch<T>
{
    Batch<T> set( String name, Object value );

    Batch<T> next();

    Batch<T> add( Map<String, Object> parameters );

    T execute();
}
