package org.mosaic.core.services;

import org.mosaic.core.modules.Module;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ServiceRegistrationException extends RuntimeException
{
    @Nonnull
    private final Module module;

    public ServiceRegistrationException( String message, @Nonnull Module module )
    {
        super( message );
        this.module = module;
    }

    public ServiceRegistrationException( String message, Throwable cause, @Nonnull Module module )
    {
        super( message, cause );
        this.module = module;
    }

    @Nonnull
    public Module getModule()
    {
        return this.module;
    }
}
