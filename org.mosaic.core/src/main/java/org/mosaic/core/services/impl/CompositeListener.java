package org.mosaic.core.services.impl;

import org.mosaic.core.services.ServiceListener;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.services.ServiceRegistrationListener;
import org.mosaic.core.services.ServiceUnregistrationListener;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
class CompositeListener<ServiceType> implements ServiceListener<ServiceType>
{
    @Nullable
    private final ServiceRegistrationListener<ServiceType> onRegister;

    @Nullable
    private final ServiceUnregistrationListener<ServiceType> onUnregister;

    CompositeListener( @Nullable ServiceRegistrationListener<ServiceType> onRegister,
                       @Nullable ServiceUnregistrationListener<ServiceType> onUnregister )
    {
        this.onRegister = onRegister;
        this.onUnregister = onUnregister;
    }

    @Override
    public final void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        if( this.onRegister != null )
        {
            this.onRegister.serviceRegistered( registration );
        }
    }

    @Override
    public final void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                           @Nonnull ServiceType service )
    {
        if( this.onUnregister != null )
        {
            this.onUnregister.serviceUnregistered( registration, service );
        }
    }
}
