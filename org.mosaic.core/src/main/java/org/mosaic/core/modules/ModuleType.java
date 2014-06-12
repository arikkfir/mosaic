package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public interface ModuleType
{
    @Nullable
    Object getInstanceFieldValue( @Nonnull String fieldName );
}
