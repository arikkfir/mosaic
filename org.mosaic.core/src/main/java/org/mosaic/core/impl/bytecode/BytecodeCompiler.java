package org.mosaic.core.impl.bytecode;

import org.mosaic.core.ModuleRevision;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
interface BytecodeCompiler
{
    @Nullable
    byte[] compile( @Nonnull ModuleRevision moduleRevision, @Nonnull WovenClass wovenClass );
}
