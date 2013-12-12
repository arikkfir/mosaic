package org.mosaic.datasource;

/**
 * @author arik
 */
public class TransactionException extends RuntimeException
{
    public TransactionException( String message )
    {
        super( message );
    }

    public TransactionException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
