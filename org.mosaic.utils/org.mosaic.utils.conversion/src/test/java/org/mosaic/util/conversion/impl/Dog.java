package org.mosaic.util.conversion.impl;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class Dog extends Animal
{
    @Nonnull
    private final String type;

    public Dog( @Nonnull String type )
    {
        super( "bark" );
        this.type = type;
    }

    @Nonnull
    public String getType()
    {
        return this.type;
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Dog dog = ( Dog ) o;
        return type.equals( dog.type );
    }

    @Override
    public int hashCode()
    {
        return type.hashCode();
    }
}
