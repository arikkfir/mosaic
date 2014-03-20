package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface TypeDescriptor
{
    @Nonnull
    Module getModule();

    @Nullable
    Object getValueForField( @Nonnull String fieldName );

    @Nonnull
    Class<?> getType();
}
