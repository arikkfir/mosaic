package org.mosaic.modules.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * @author arik
 */
abstract class BytecodeCompiler
{
    @Nullable
    abstract byte[] compile( @Nonnull WovenClass wovenClass );
}
