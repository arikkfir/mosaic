package org.mosaic.lifecycle;

import com.google.common.reflect.TypeToken;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * @author arik
 */
public interface Module
{
    long getId();

    @Nonnull
    String getName();

    @Nonnull
    String getVersion();

    @Nonnull
    Path getPath();

    @Nonnull
    Map<String, String> getHeaders();

    @Nonnull
    ModuleState getState();

    void waitForActivation( @Nonnull Duration timeout ) throws InterruptedException;

    long getLastModified();

    @Nullable
    URL getResource( @Nonnull String name );

    @Nullable
    ClassLoader getClassLoader();

    @Nonnull
    Collection<String> getResources();

    @Nonnull
    Collection<ServiceExport> getExportedServices();

    @Nonnull
    Collection<ServiceExport> getImportedServices();

    @Nonnull
    Collection<PackageExport> getExportedPackages();

    @Nonnull
    Collection<PackageExport> getImportedPackages();

    @Nonnull
    <T> ServiceExport exportService( @Nonnull Class<? super T> type, T service, @Nonnull DP... properties );

    void start() throws ModuleStartException;

    void stop() throws ModuleStopException;

    @Nonnull
    <T> T getBean( @Nonnull Class<? extends T> type );

    @Nonnull
    <T> T getBean( @Nonnull String beanName, @Nonnull Class<? extends T> type );

    @Nullable
    Metrics getMetrics();

    interface Metrics
    {
        @Nonnull
        MetricsTimer getTimer( @Nonnull String group, @Nonnull String type, @Nonnull String name );

        @Nonnull
        Collection<? extends MetricsTimer> getTimers();
    }

    interface MetricsTimer
    {
        @Nonnull
        Module getModule();

        @Nonnull
        String getName();

        void startTimer();

        void stopTimer();

        long count();

        double fifteenMinuteRate();

        double fiveMinuteRate();

        double meanRate();

        double oneMinuteRate();

        double max();

        double min();

        double mean();

        double stdDev();

        double sum();
    }

    interface ServiceExport
    {
        @Nonnull
        Module getProvider();

        @Nonnull
        TypeToken<?> getType();

        @Nonnull
        Map<String, Object> getProperties();

        @Nonnull
        Collection<Module> getConsumers();

        boolean isRegistered();

        void unregister();
    }

    interface PackageExport
    {
        @Nonnull
        Module getProvider();

        @Nonnull
        String getPackageName();

        @Nonnull
        String getVersion();
    }
}
