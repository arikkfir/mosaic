package org.mosaic.core.modules;

import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ModuleManager
{
    @Nullable
    Module getModule( long id );

    @Nonnull
    Collection<? extends Module> getModules();
}
