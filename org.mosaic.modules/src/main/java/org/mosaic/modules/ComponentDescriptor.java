package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ComponentDescriptor<Type>
{
    @Nullable
    Object getValueForField( @Nonnull String fieldName );

    @Nonnull
    Type getInstance();
}
