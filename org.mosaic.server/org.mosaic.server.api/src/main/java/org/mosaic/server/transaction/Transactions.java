package org.mosaic.server.transaction;

import org.mosaic.server.transaction.demarcation.impl.TransactionDemarcator;

/**
 * @author arik
 */
public abstract class Transactions
{

    private static final TransactionDemarcator DEMARCATOR = new TransactionDemarcator( );

    @SuppressWarnings( "UnusedDeclaration" )
    public static void begin( String transactionName, Object transactional )
    {
        DEMARCATOR.begin( transactionName, transactional );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static void rollback( )
    {
        DEMARCATOR.rollback( );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public static void finish( )
    {
        DEMARCATOR.rollback( );
    }

}
