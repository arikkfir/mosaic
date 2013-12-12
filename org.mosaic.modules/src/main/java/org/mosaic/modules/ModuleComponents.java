package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ModuleComponents
{
    @Nonnull
    Class<?> loadClass( @Nonnull String className ) throws ClassNotFoundException;

    @Nonnull
    <T> T getComponent( @Nonnull Class<T> type );

    @Nullable
    <T> ComponentDescriptor<T> getComponentDescriptor( @Nonnull Class<T> type );
}
