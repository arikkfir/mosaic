package org.mosaic.core.impl.module;

import org.mosaic.core.ModuleManager;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author arik
 */
public interface ModuleManagerEx extends ModuleManager
{
    @Nullable
    ModuleRevision getModuleRevision( @Nonnull BundleRevision bundleRevision );
}
