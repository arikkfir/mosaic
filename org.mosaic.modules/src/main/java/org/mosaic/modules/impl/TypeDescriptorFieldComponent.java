package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.ComponentDefinitionException;

/**
 * @author arik
 */
class TypeDescriptorFieldComponent extends TypeDescriptorField
{
    TypeDescriptorFieldComponent( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Field field )
    {
        super( typeDescriptor, field );
    }

    @Override
    public String toString()
    {
        return "TypeDescriptorFieldComponent[" + super.toString() + "]";
    }

    @Nonnull
    Class<?> getFieldType()
    {
        return this.field.getType();
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        // TODO: we can cache @Component(s) list, it won't change

        List<TypeDescriptor> typeDescriptors = typeDescriptor.getModule().getTypeDescriptors( getFieldType() );
        List<Object> components = null;
        for( TypeDescriptor typeDescriptor : typeDescriptors )
        {
            Component component = typeDescriptor.getChild( Component.class );
            if( component != null )
            {
                Object instance = component.getInstance();
                if( instance == null )
                {
                    throw new IllegalStateException( "component " + component + " has no instance" );
                }
                else if( components == null )
                {
                    components = new LinkedList<>();
                }
                components.add( instance );
            }
        }

        if( components == null || components.isEmpty() )
        {
            throw new ComponentDefinitionException( "type '" + this.typeDescriptor + "' requires @Component list of '" + this.field.getType() + "' but none were found",
                                                    this.typeDescriptor.getType(),
                                                    this.typeDescriptor.getModule() );
        }
        else if( components.size() > 1 )
        {
            throw new ComponentDefinitionException( "type '" + this.typeDescriptor + "' requires @Component of '" + this.field.getType() + "' but more than one was found",
                                                    this.typeDescriptor.getType(),
                                                    this.typeDescriptor.getModule() );
        }
        else
        {
            return components.get( 0 );
        }
    }
}
