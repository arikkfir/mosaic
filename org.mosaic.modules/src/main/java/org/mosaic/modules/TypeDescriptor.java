package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface TypeDescriptor
{
    @Nullable
    Object getValueForField( @Nonnull String fieldName );
}
