package org.mosaic.core;

import java.nio.file.Path;
import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

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

    @Nonnull
    <ServiceType> ServiceTracker<ServiceType> createServiceTracker( @Nonnull Class<ServiceType> type,
                                                                    @Nonnull ServiceProperty... properties );

    void start();

    void refresh();

    void stop();

    void uninstall();
}
