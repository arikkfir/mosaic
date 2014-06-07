package org.mosaic.core.impl.module;

import org.mosaic.core.ServiceTracker;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;

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
        this.serviceTracker =
                ( ServiceTracker<ServiceType> ) moduleRevision.getModule().createServiceTracker(
                        this.serviceKey.getServiceType().getErasedType(),
                        this.serviceKey.getServicePropertiesArray()
                );
        this.serviceTracker.addEventHandler(
                reg -> {
                    if( this.serviceTracker.getServices().size() >= this.serviceKey.getMinCount() )
                    {
                        notifySatisfaction();
                    }
                },
                ( reg, service ) -> {
                    if( this.serviceTracker.getServices().size() < this.serviceKey.getMinCount() )
                    {
                        notifyUnsatisfaction();
                    }
                } );
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
}
