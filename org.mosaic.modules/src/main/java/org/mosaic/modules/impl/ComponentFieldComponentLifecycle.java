package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
final class ComponentFieldComponentLifecycle extends ComponentField
{
    ComponentFieldComponentLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor, @Nonnull Field field )
    {
        super( componentDescriptor, field );
    }

    @Nullable
    @Override
    protected String toStringInternal()
    {
        return "@Component '" + super.toStringInternal() + "'";
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        return this.componentDescriptor.getModule().getModuleComponents().getComponent( this.field.getType() );
    }
}
