package org.mosaic.core.weaving.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
interface BytecodeCompiler
{
    @Nullable
    byte[] compile( @Nonnull WovenClass wovenClass );
}
