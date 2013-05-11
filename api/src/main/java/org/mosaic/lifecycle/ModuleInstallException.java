package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ModuleInstallException extends Exception
{
    public ModuleInstallException( @Nonnull String location, @Nonnull String message )
    {
        super( "could not install module from '" + location + "': " + message );
    }

    public ModuleInstallException( @Nonnull String location, @Nonnull Throwable cause )
    {
        super( "could not install module from '" + location + "': " + cause.getMessage(), cause );
    }
}
