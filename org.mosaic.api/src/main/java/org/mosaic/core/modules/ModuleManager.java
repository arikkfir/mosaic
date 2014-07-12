package org.mosaic.core.modules;

import java.nio.file.Path;
import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ModuleManager
{
    @Nonnull
    Module installModule( @Nonnull Path path );

    @Nullable
    Module getModule( long id );

    @Nullable
    Module getModule( @Nonnull String name );

    @Nonnull
    Collection<? extends Module> getModules();
}
