package org.mosaic.core.modules;

import java.nio.file.Path;
import java.util.Collection;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;

/**
 * @author arik
 */
public interface Module
{
    class ServiceProperty
    {
        @Nonnull
        public static ServiceProperty p( @Nonnull String name, @Nonnull Object value )
        {
            return new ServiceProperty( name, value );
        }

        @Nonnull
        private final String name;

        @Nonnull
        private final Object value;

        private ServiceProperty( @Nonnull String name, @Nonnull Object value )
        {
            this.name = name;
            this.value = value;
        }

        @Nonnull
        public String getName()
        {
            return name;
        }

        @Nonnull
        public Object getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "name", this.name )
                                 .add( "value", this.value )
                                 .toString();
        }
    }

    long getId();

    @Nullable
    Path getPath();

    @Nonnull
    ModuleState getState();

    @Nullable
    ModuleRevision getCurrentRevision();

    @Nonnull
    Collection<ModuleRevision> getRevisions();

    @Nullable
    ModuleRevision getRevision( long revisionId );

    @Nonnull
    <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceType service,
                                                                    @Nonnull ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull Module.ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                                                               @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                               @Nonnull Class<ServiceType> type,
                                                                               @Nonnull Module.ServiceProperty... properties );

    <ServiceType> ServiceListenerRegistration<ServiceType> addWeakServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                   @Nonnull Class<ServiceType> type,
                                                                                   @Nonnull Module.ServiceProperty... properties );

    void start();

    void refresh();

    void stop();

    void uninstall();
}
