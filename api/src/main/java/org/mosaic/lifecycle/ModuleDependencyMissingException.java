package org.mosaic.lifecycle;

/**
 * @author arik
 */
public class ModuleDependencyMissingException extends ModuleStartException
{
    private final Module module;

    public ModuleDependencyMissingException( Module module )
    {
        super( module, "has unsatisfied dependencies" );
        this.module = module;
    }

    public Module getModule()
    {
        return module;
    }
}
