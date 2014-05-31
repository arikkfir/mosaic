package org.mosaic.core.impl.module;

import org.mosaic.core.ServiceListener;
import org.mosaic.core.ServiceManager;
import org.mosaic.core.ServiceRegistration;
import org.mosaic.core.ServiceTracker;
import org.mosaic.core.impl.Activator;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

import static java.util.Objects.requireNonNull;

/**
 * @author arik
 */
class ModuleRevisionImplServiceDependency<ServiceType> extends ModuleRevisionImplDependency
{
    @Nonnull
    private final ServiceKey serviceKey;

    @Nonnull
    private final ServiceTracker<ServiceType> serviceTracker;

    @SuppressWarnings( "unchecked" )
    ModuleRevisionImplServiceDependency( @Nonnull ModuleRevisionImpl moduleRevision,
                                         @Nonnull ServiceKey serviceKey )
    {
        super( moduleRevision );
        this.serviceKey = serviceKey;

        ServiceManager serviceManager = requireNonNull( Activator.getServiceManager() );
        this.serviceTracker =
                ( ServiceTracker<ServiceType> ) serviceManager.createServiceTracker(
                        this.serviceKey.getServiceType().getErasedType(),
                        this.serviceKey.getServicePropertiesArray()
                );
        this.serviceTracker.addEventHandler( new DependencySatisfactionSynchronizer() );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( "ServiceDependency" )
                             .add( "key", this.serviceKey )
                             .toString();
    }

    @Nonnull
    final ServiceKey getServiceKey()
    {
        return this.serviceKey;
    }

    @Nonnull
    ServiceTracker<ServiceType> getServiceTracker()
    {
        return this.serviceTracker;
    }

    @Override
    void initialize()
    {
        this.serviceTracker.startTracking();
        if( this.serviceTracker.getServices().size() >= this.serviceKey.getMinCount() )
        {
            notifySatisfaction();
        }
        else
        {
            notifyUnsatisfaction();
        }
    }

    @Override
    void shutdown()
    {
        this.serviceTracker.stopTracking();
    }

    @SuppressWarnings( "unchecked" )
    private class DependencySatisfactionSynchronizer implements ServiceListener<ServiceType>
    {
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            ServiceKey serviceKey = ModuleRevisionImplServiceDependency.this.serviceKey;

            ServiceTracker<ServiceType> serviceTracker = ModuleRevisionImplServiceDependency.this.serviceTracker;
            if( serviceTracker.getServices().size() >= serviceKey.getMinCount() )
            {
                notifySatisfaction();
            }
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            ServiceKey serviceKey = ModuleRevisionImplServiceDependency.this.serviceKey;

            ServiceTracker<ServiceType> serviceTracker = ModuleRevisionImplServiceDependency.this.serviceTracker;
            if( serviceTracker.getServices().size() < serviceKey.getMinCount() )
            {
                notifyUnsatisfaction();
            }
        }
    }
}
