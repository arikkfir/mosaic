package org.mosaic.lifecycle;

/**
 * @author arik
 */
public class ModuleStopException extends Exception
{
    private final Module module;

    public ModuleStopException( Module module, String message )
    {
        this( module, message, null );
    }

    public ModuleStopException( Module module, String message, Throwable cause )
    {
        super( "Could not stop packages for module '" + module.getName() + "-" + module.getVersion() + "[" + module.getId() + "]': " + message, cause );
        this.module = module;
    }

    public Module getModule()
    {
        return module;
    }
}
