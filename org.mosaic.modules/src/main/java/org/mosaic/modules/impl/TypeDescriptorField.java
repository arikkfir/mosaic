package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
abstract class TypeDescriptorField extends Lifecycle
{
    @Nonnull
    protected final TypeDescriptor typeDescriptor;

    @Nonnull
    protected final Field field;

    TypeDescriptorField( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Field field )
    {
        this.typeDescriptor = typeDescriptor;
        this.field = field;
    }

    @Override
    public String toString()
    {
        return this.field.getName() + ":" + this.field.getType().getSimpleName();
    }

    @Nonnull
    Field getField()
    {
        return this.field;
    }

    @Nullable
    protected abstract Object getValue();
}
