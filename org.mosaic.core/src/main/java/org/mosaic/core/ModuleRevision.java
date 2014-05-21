package org.mosaic.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.version.Version;

/**
 * @author arik
 */
public interface ModuleRevision
{
    @Nonnull
    Module getModule();

    long getId();

    @Nonnull
    String getName();

    @Nonnull
    Version getVersion();

    @Nonnull
    Map<String, String> getHeaders();

    boolean isCurrent();

    @Nullable
    ClassLoader getClassLoader();

    @Nullable
    Path findResource( @Nonnull String glob ) throws IOException;

    @Nonnull
    Collection<Path> findResources( @Nonnull String glob ) throws IOException;

    @Nullable
    ModuleType getType( @Nonnull Class<?> type );
}
