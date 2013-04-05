package org.mosaic.database.tx;

/**
 * @author arik
 */
public class NoTransactionException extends TransactionException
{
    public NoTransactionException()
    {
        super( "No active transaction" );
    }
}
