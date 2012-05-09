package org.mosaic.cms;

/**
 * @author arik
 */
public class MissingRequiredDataException extends Exception
{
    public MissingRequiredDataException( String message )
    {
        super( message );
    }

    public MissingRequiredDataException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
