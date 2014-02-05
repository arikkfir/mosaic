package org.mosaic.modules;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.version.Version;

/**
 * @author arik
 */
public interface Module
{
    long getId();

    @Nonnull
    String getName();

    @Nonnull
    Version getVersion();

    @Nonnull
    Path getPath();

    @Nonnull
    Path getLocation();

    @Nonnull
    MapEx<String, String> getHeaders();

    @Nonnull
    DateTime getLastModified();

    @Nonnull
    ModuleState getState();

    @Nonnull
    Optional<Path> findResource( @Nonnull String glob ) throws IOException;

    @Nonnull
    Collection<Path> findResources( @Nonnull String glob ) throws IOException;

    @Nonnull
    ClassLoader getClassLoader();

    @Nullable
    TypeDescriptor getTypeDescriptor( @Nonnull Class<?> type );

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

    void startModule() throws ModuleStartException;

    void stopModule() throws ModuleStopException;

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
