package org.mosaic.server.transaction;

/**
 * @author arik
 */
public interface TransactionManager
{

    Object begin( String name );

    void rollback( Object tx );

    void commit( Object tx );

}
