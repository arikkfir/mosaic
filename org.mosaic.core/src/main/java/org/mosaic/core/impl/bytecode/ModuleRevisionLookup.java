package org.mosaic.core.impl.bytecode;

import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.wiring.BundleRevision;

/**
 * @author arik
 */
public interface ModuleRevisionLookup
{
    @Nullable
    ModuleRevision getModuleRevision( @Nonnull BundleRevision bundleRevision );
}
