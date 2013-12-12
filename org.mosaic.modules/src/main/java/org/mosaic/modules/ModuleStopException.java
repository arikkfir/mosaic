package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ModuleStopException extends ModuleException
{
    public ModuleStopException( @Nonnull String message, @Nullable Module module )
    {
        super( "could not stop module " + module + ": " + message, module );
    }

    public ModuleStopException( @Nonnull Throwable cause, @Nullable Module module )
    {
        super( "could not stop module " + module + ": " + cause.getMessage(), cause, module );
    }
}
