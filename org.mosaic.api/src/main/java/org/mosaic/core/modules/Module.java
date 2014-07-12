package org.mosaic.core.modules;

import java.nio.file.Path;
import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface Module
{
    long getId();

    @Nullable
    Path getPath();

    @Nonnull
    ModuleState getState();

    @Nullable
    ModuleRevision getCurrentRevision();

    @Nonnull
    Collection<ModuleRevision> getRevisions();

    @Nullable
    ModuleRevision getRevision( long revisionId );

    void start();

    void refresh();

    void stop();

    void uninstall();
}
