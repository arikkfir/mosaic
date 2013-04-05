package org.mosaic.lifecycle;

/**
 * @author arik
 */
public class ModuleResolveException extends ModuleStartException
{
    private final Module module;

    public ModuleResolveException( Module module, Throwable cause )
    {
        super( module, "could not resolve packages", cause );
        this.module = module;
    }

    public Module getModule()
    {
        return module;
    }
}
