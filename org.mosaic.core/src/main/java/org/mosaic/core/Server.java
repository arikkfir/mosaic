package org.mosaic.core;

import java.nio.file.Path;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.version.Version;

/**
 * @author arik
 */
public interface Server
{
    @Nonnull
    Version getVersion();

    @Nonnull
    Path getHome();

    @Nonnull
    Path getApps();

    @Nonnull
    Path getBin();

    @Nonnull
    Path getEtc();

    @Nonnull
    Path getLib();

    @Nonnull
    Path getLogs();

    @Nonnull
    Path getSchemas();

    @Nonnull
    Path getWork();
}
