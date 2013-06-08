package org.mosaic.idea.module.actions;

import com.intellij.openapi.module.Module;

/**
 * @author arik
 */
public class ModuleBuildException extends RuntimeException
{
    private final Module module;

    public ModuleBuildException( String message, Module module )
    {
        super( message );
        this.module = module;
    }

    public ModuleBuildException( String message, Throwable cause, Module module )
    {
        super( message, cause );
        this.module = module;
    }

    public Module getModule()
    {
        return module;
    }
}
