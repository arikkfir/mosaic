package org.mosaic.modules;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface ModuleWiring
{
    @Nonnull
    Module getModule();

    @Nonnull
    Collection<PackageRequirement> getPackageRequirements();

    @Nonnull
    Collection<PackageCapability> getPackageCapabilities();

    @Nonnull
    Collection<ServiceRequirement> getServiceRequirements();

    @Nonnull
    Collection<ServiceCapability> getServiceCapabilities();

    @Nullable
    <T> ServiceReference<T> findService( @Nonnull Class<T> type, @Nonnull Property... properties );

    @Nonnull
    <T> ServiceRegistration<T> register( @Nonnull Class<T> type, @Nonnull T service, @Nonnull Property... properties );

    interface PackageRequirement
    {
        @Nonnull
        Module getConsumer();

        @Nonnull
        String getPackageName();

        @Nullable
        String getFilter();

        boolean isOptional();

        @Nullable
        Module getProvider();

        @Nullable
        Version getVersion();
    }

    interface PackageCapability
    {
        @Nonnull
        Module getProvider();

        @Nonnull
        String getPackageName();

        @Nonnull
        Version getVersion();

        @Nonnull
        Collection<Module> getConsumers();
    }

    interface ServiceRequirement
    {
        @Nonnull
        Module getConsumer();

        @Nonnull
        Class<?> getType();

        @Nullable
        String getFilter();

        @Nonnull
        List<ServiceReference<?>> getReferences();
    }

    interface ServiceCapability
    {
        long getId();

        @Nonnull
        Module getProvider();

        @Nonnull
        Class<?> getType();

        @Nonnull
        MapEx<String, Object> getProperties();

        @Nonnull
        Collection<Module> getConsumers();
    }
}
