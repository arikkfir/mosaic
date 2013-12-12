package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
abstract class ComponentField extends Lifecycle
{
    @Nonnull
    protected final ComponentDescriptorImpl<?> componentDescriptor;

    @Nonnull
    protected final Field field;

    ComponentField( @Nonnull ComponentDescriptorImpl<?> componentDescriptor, @Nonnull Field field )
    {
        this.componentDescriptor = componentDescriptor;
        this.field = field;
    }

    @Override
    public final String toString()
    {
        return "ComponentField[" + toStringInternal() + "]";
    }

    @Nonnull
    Field getField()
    {
        return this.field;
    }

    @Nullable
    protected String toStringInternal()
    {
        return this.field.getType().getSimpleName() + " " + this.field.getName();
    }

    @Nullable
    protected abstract Object getValue();
}
