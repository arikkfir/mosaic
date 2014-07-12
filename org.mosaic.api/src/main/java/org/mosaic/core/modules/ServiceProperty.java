package org.mosaic.core.modules;

import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public class ServiceProperty
{
    @Nonnull
    public static ServiceProperty p( @Nonnull String name, @Nonnull Object value )
    {
        return new ServiceProperty( name, value );
    }

    @Nonnull
    private final String name;

    @Nonnull
    private final Object value;

    private ServiceProperty( @Nonnull String name, @Nonnull Object value )
    {
        this.name = name;
        this.value = value;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    @Nonnull
    public Object getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "Property[" + this.name + "=" + this.value + "]";
    }
}
