package org.mosaic.core.modules.impl;

import org.mosaic.core.modules.ModuleManager;
import org.mosaic.core.modules.ModuleRevision;
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
