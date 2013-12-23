package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;

/**
 * @author arik
 */
final class TypeDescriptorFieldModule extends TypeDescriptorField
{
    TypeDescriptorFieldModule( @Nonnull TypeDescriptor componentDescriptor, @Nonnull Field field )
    {
        super( componentDescriptor, field );
    }

    @Nullable
    @Override
    public String toString()
    {
        return "TypeDescriptorFieldModule[" + super.toString() + "]";
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        if( this.field.getType().isAssignableFrom( Module.class ) )
        {
            return this.typeDescriptor.getModule();
        }
        else if( this.field.getType().isAssignableFrom( ModuleTypes.class ) )
        {
            return this.typeDescriptor.getModule().getModuleTypes();
        }
        else if( this.field.getType().isAssignableFrom( ModuleContext.class ) )
        {
            return this.typeDescriptor.getModule().getContext();
        }
        else if( this.field.getType().isAssignableFrom( ModuleResources.class ) )
        {
            return this.typeDescriptor.getModule().getModuleResources();
        }
        else if( this.field.getType().isAssignableFrom( ModuleWiring.class ) )
        {
            return this.typeDescriptor.getModule().getModuleWiring();
        }
        else
        {
            throw new IllegalStateException( "illegal field type '" + this.field.getType().getName() + "' for @Component" );
        }
    }
}
