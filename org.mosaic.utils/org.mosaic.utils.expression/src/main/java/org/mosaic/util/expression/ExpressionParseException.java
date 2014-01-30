package org.mosaic.util.expression;

/**
 * @author arik
 */
public class ExpressionParseException extends RuntimeException
{
    public ExpressionParseException( String message )
    {
        super( message );
    }

    public ExpressionParseException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
