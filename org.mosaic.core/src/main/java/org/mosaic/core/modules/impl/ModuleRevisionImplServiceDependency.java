package org.mosaic.core.modules.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.mosaic.core.services.ServiceListener;
import org.mosaic.core.services.ServiceListenerRegistration;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.services.ServicesProvider;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;

import static java.util.Objects.requireNonNull;
import static org.mosaic.core.impl.Activator.getDispatcher;

/**
 * @author arik
 */
class ModuleRevisionImplServiceDependency<ServiceType> extends ModuleRevisionImplDependency
        implements ServicesProvider<ServiceType>, ServiceListener<ServiceType>
{
    @Nonnull
    private final ServiceKey serviceKey;

    @Nonnull
    private final List<ServiceRegistration<ServiceType>> registrations = new LinkedList<>();

    @Nonnull
    private final List<ServiceType> services = new LinkedList<>();

    @Nullable
    private ServiceListenerRegistration<ServiceType> listenerRegistration;

    @SuppressWarnings("unchecked")
    ModuleRevisionImplServiceDependency( @Nonnull ModuleRevisionImpl moduleRevision,
                                         @Nonnull ServiceKey serviceKey )
    {
        super( moduleRevision );
        this.serviceKey = serviceKey;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( "ServiceDependency" )
                             .add( "key", this.serviceKey )
                             .toString();
    }

    @Override
    public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> {
            ServiceType service = registration.getService();
            if( service != null )
            {
                this.registrations.add( registration );
                this.services.add( service );
                if( this.services.size() >= this.serviceKey.getMinCount() )
                {
                    notifySatisfaction();
                }
            }
        } );

    }

    @Override
    public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                     @Nonnull ServiceType service )
    {
        requireNonNull( getDispatcher() ).dispatch( () -> {
            this.registrations.remove( registration );
            this.services.remove( service );
            if( this.services.size() < this.serviceKey.getMinCount() )
            {
                notifyUnsatisfaction();
            }
        } );
    }

    @Nonnull
    @Override
    public List<ServiceRegistration<ServiceType>> getRegistrations()
    {
        return this.moduleRevision.getModule().getLock().read( () -> new LinkedList<>( this.registrations ) );
    }

    @Nonnull
    @Override
    public List<ServiceType> getServices()
    {
        return this.moduleRevision.getModule().getLock().read( () -> new LinkedList<>( this.services ) );
    }

    @Nonnull
    final ServiceKey getServiceKey()
    {
        return this.serviceKey;
    }

    @Override
    @SuppressWarnings({ "unchecked cast", "unchecked" })
    void initialize()
    {
        notifyUnsatisfaction();
        this.listenerRegistration = this.moduleRevision.getModule().addServiceListener(
                this,
                ( Class<ServiceType> ) this.serviceKey.getServiceType().getErasedType(),
                this.serviceKey.getServicePropertiesArray()
        );
    }

    @Override
    void shutdown()
    {
        ServiceListenerRegistration<ServiceType> registration = this.listenerRegistration;
        if( registration != null )
        {
            registration.unregister();
        }
    }

    <T> T doWithServices( @Nonnull Function<List<ServiceType>, T> function )
    {
        return this.moduleRevision.getModule().getLock().read( () -> function.apply( this.services ) );
    }

    <T> T doWithRegistrations( @Nonnull Function<List<ServiceRegistration<ServiceType>>, T> function )
    {
        return this.moduleRevision.getModule().getLock().read( () -> function.apply( this.registrations ) );
    }
}
