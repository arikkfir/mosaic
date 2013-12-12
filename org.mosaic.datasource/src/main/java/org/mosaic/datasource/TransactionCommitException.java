package org.mosaic.datasource;

/**
 * @author arik
 */
public class TransactionCommitException extends TransactionException
{
    public TransactionCommitException( String message )
    {
        super( message );
    }

    public TransactionCommitException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
