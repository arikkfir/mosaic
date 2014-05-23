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
    private final ServiceKey<ServiceType> serviceKey;

    @Nonnull
    private final ServiceTracker<ServiceType> serviceTracker;

    ModuleRevisionImplServiceDependency( @Nonnull ModuleRevisionImpl moduleRevision,
                                         @Nonnull ServiceKey<ServiceType> serviceKey )
    {
        super( moduleRevision );
        this.serviceKey = serviceKey;

        ServiceManager serviceManager = requireNonNull( Activator.getServiceManager() );
        this.serviceTracker = serviceManager.createServiceTracker( this.serviceKey.getServiceType(), this.serviceKey.getServicePropertiesArray() );
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
    final ServiceKey<ServiceType> getServiceKey()
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

    @SuppressWarnings("unchecked")
    private class DependencySatisfactionSynchronizer implements ServiceListener<ServiceType>
    {
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            ServiceKey<ServiceType> serviceKey = ModuleRevisionImplServiceDependency.this.serviceKey;

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
            ServiceKey<ServiceType> serviceKey = ModuleRevisionImplServiceDependency.this.serviceKey;

            ServiceTracker<ServiceType> serviceTracker = ModuleRevisionImplServiceDependency.this.serviceTracker;
            if( serviceTracker.getServices().size() < serviceKey.getMinCount() )
            {
                notifyUnsatisfaction();
            }
        }
    }
}
