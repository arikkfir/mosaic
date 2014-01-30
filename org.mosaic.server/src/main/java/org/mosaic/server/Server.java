package org.mosaic.server;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import org.mosaic.util.version.Version;

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
