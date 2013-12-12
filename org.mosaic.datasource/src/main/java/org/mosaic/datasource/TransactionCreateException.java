package org.mosaic.datasource;

/**
 * @author arik
 */
public class TransactionCreateException extends TransactionException
{
    public TransactionCreateException( String message )
    {
        super( message );
    }

    public TransactionCreateException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
