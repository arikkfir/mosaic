package org.mosaic.lifecycle;

/**
 * @author arik
 */
public class ModuleStartException extends Exception
{
    private final Module module;

    public ModuleStartException( Module module, String message )
    {
        this( module, message, null );
    }

    public ModuleStartException( Module module, String message, Throwable cause )
    {
        super( "Could not activate " + module + ": " + message, cause );
        this.module = module;
    }

    public Module getModule()
    {
        return module;
    }
}
