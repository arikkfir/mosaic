package org.mosaic.event;

import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author arik
 */
public final class EventProperty extends Pair<String, Object>
{
    @Nonnull
    public static EventProperty eventProperty( @Nonnull String name, @Nonnull Object value )
    {
        return new EventProperty( name, value );
    }

    @Nonnull
    private final String name;

    @Nonnull
    private final Object value;

    private EventProperty( @Nonnull String name, @Nonnull Object value )
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
