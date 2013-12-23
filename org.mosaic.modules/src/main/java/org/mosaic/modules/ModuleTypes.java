package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ModuleTypes
{
    @Nonnull
    Class<?> loadClass( @Nonnull String className ) throws ClassNotFoundException;

    @Nullable
    TypeDescriptor getTypeDescriptor( @Nonnull Class<?> type );
}
