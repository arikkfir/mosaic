package org.mosaic.modules;

import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ModuleContext
{
    @Nonnull
    Version getServerVersion();

    boolean isDevelopmentMode();

    @Nonnull
    Path getServerHome();

    @Nonnull
    Path getServerAppsHome();

    @Nonnull
    Path getServerEtcHome();

    @Nonnull
    Path getServerLibHome();

    @Nonnull
    Path getServerLogsHome();

    @Nonnull
    Path getServerWorkHome();
}
