package org.mosaic;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Server
{
    @Nonnull
    String getVersion();

    @Nonnull
    Path getHome();

    @Nonnull
    Path getApps();

    @Nonnull
    Path getEtc();

    @Nonnull
    Path getLib();

    @Nonnull
    Path getLogs();

    @Nonnull
    Path getWork();

    void shutdown();

    void restart();
}
