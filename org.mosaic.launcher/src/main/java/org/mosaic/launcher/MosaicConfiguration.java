package org.mosaic.launcher;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface MosaicConfiguration
{
    @Nonnull
    String getVersion();

    boolean isDevMode();

    @Nonnull
    Path getHome();

    @Nonnull
    Path getBoot();

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
}
