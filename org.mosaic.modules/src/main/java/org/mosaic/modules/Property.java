package org.mosaic.modules;

import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author arik
 */
public final class Property extends Pair<String, Object>
{
    @Nonnull
    public static Property property( @Nonnull String name, @Nonnull Object value )
    {
        return new Property( name, value );
    }

    @Nonnull
    private final String name;

    @Nonnull
    private final Object value;

    private Property( @Nonnull String name, @Nonnull Object value )
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getLeft()
    {
        return this.name;
    }

    @Override
    public Object getRight()
    {
        return this.value;
    }

    @Override
    public Object setValue( Object value )
    {
        throw new UnsupportedOperationException();
    }
}
