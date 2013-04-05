package org.mosaic.lifecycle;

import com.google.common.reflect.TypeToken;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;

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
    <T> ServiceExport exportService( @Nonnull Class<? super T> type, T service, @Nonnull DP... properties );

    void start() throws ModuleStartException;

    void stop() throws ModuleStopException;

    @Nullable
    Metrics getMetrics();

    interface Metrics
    {
        @Nonnull
        MetricsTimer getTimer( @Nonnull MethodHandle method );

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
}