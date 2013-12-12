package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class ModuleStartException extends ModuleException
{
    public ModuleStartException( @Nonnull String message, @Nullable Module module )
    {
        super( "could not start module " + module + ": " + message, module );
    }

    public ModuleStartException( @Nonnull Throwable cause, @Nullable Module module )
    {
        super( "could not start module " + module + ": " + cause.getMessage(), cause, module );
    }
}
