package org.mosaic.core.modules;

/**
 * @author arik
 */
public class ModuleInstallException extends RuntimeException
{
    public ModuleInstallException( String message )
    {
        super( message );
    }

    public ModuleInstallException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
