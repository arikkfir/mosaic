package org.mosaic.modules;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface Module
{
    @Nonnull
    ModuleContext getContext();

    long getId();

    @Nonnull
    String getName();

    @Nonnull
    Version getVersion();

    @Nonnull
    Path getPath();

    @Nonnull
    MapEx<String, String> getHeaders();

    @Nonnull
    DateTime getLastModified();

    @Nonnull
    ModuleState getState();

    @Nonnull
    ModuleResources getModuleResources();

    @Nonnull
    ModuleComponents getModuleComponents();

    @Nonnull
    ModuleWiring getModuleWiring();

    void startModule() throws ModuleStartException;

    void stopModule() throws ModuleStopException;
}