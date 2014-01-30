package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ComponentDefinitionException;
import org.mosaic.util.reflection.TypeTokens;

/**
 * @author arik
 */
class TypeDescriptorFieldComponentList extends TypeDescriptorField
{
    @Nonnull
    private final Class<?> listItemType;

    TypeDescriptorFieldComponentList( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Field field )
    {
        super( typeDescriptor, field );
        this.listItemType = TypeTokens.ofList( this.field.getGenericType() ).getRawType();
    }

    @Nullable
    @Override
    public String toString()
    {
        return "TypeDescriptorFieldComponentList[" + super.toString() + "]";
    }

    @Nonnull
    Class<?> getFieldListItemType()
    {
        return this.listItemType;
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        // TODO: we can cache @Component(s) list, it won't change

        List<TypeDescriptor> typeDescriptors = this.typeDescriptor.getModule().getTypeDescriptors( this.listItemType );

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
        else
        {
            return components;
        }
    }
}
