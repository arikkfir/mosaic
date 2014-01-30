package org.mosaic.util.expression;

/**
 * @author arik
 */
public class ExpressionEvaluateException extends RuntimeException
{
    public ExpressionEvaluateException( String message )
    {
        super( message );
    }

    public ExpressionEvaluateException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
