package org.mosaic.datasource;

/**
 * @author arik
 */
public class TransactionRollbackException extends TransactionException
{
    public TransactionRollbackException( String message )
    {
        super( message );
    }

    public TransactionRollbackException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
