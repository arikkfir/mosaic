package org.mosaic.server;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface Server
{
    @Nonnull
    Version getVersion();

    boolean isDevelopmentMode();

    @Nonnull
    Path getHome();

    @Nonnull
    Path getBootPath();

    @Nonnull
    Path getAppsPath();

    @Nonnull
    Path getEtcPath();

    @Nonnull
    Path getLibPath();

    @Nonnull
    Path getLogsPath();

    @Nonnull
    Path getWorkPath();
}
