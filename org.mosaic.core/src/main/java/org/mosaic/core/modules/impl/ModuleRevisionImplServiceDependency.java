package org.mosaic.core.modules.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
class ModuleRevisionImplServiceDependency<ServiceType> extends ModuleRevisionImplDependency
        implements ServicesProvider<ServiceType>,
                   ServiceProvider<ServiceType>,
                   ServiceListener<ServiceType>
{
    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final ServiceKey serviceKey;

    @Nonnull
    private final List<ServiceRegistration<ServiceType>> registrations = new LinkedList<>();

    @Nonnull
    private final List<ServiceType> services = new LinkedList<>();

    @Nullable
    private ServiceListenerRegistration<ServiceType> listenerRegistration;

    @SuppressWarnings("unchecked")
    ModuleRevisionImplServiceDependency( @Nonnull ServerImpl server,
                                         @Nonnull ModuleRevisionImpl moduleRevision,
                                         @Nonnull ServiceKey serviceKey )
    {
        super( moduleRevision );
        this.server = server;
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
        this.server.getLock().write( () -> {
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
        this.server.getLock().write( () -> {
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
        return this.server.getLock().read( () -> new LinkedList<>( this.registrations ) );
    }

    @Nonnull
    @Override
    public List<ServiceType> getServices()
    {
        return this.server.getLock().read( () -> new LinkedList<>( this.services ) );
    }

    @Nullable
    @Override
    public ServiceRegistration<ServiceType> getRegistration()
    {
        return this.server.getLock().read( () -> this.registrations.isEmpty() ? null : this.registrations.get( 0 ) );
    }

    @Nullable
    @Override
    public ServiceType getService()
    {
        return this.server.getLock().read( () -> this.services.isEmpty() ? null : this.services.get( 0 ) );
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
        this.listenerRegistration = this.moduleRevision.addServiceListener(
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
        return this.server.getLock().read( () -> function.apply( this.services ) );
    }

    <T> T doWithRegistrations( @Nonnull Function<List<ServiceRegistration<ServiceType>>, T> function )
    {
        // TODO: this method will be used once ModuleTypeImpl will support List<ServiceRegistration?>> injected dependencies
        return this.server.getLock().read( () -> function.apply( this.registrations ) );
    }
}
