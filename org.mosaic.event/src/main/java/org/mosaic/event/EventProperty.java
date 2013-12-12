package org.mosaic.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.pair.Pair;

/**
 * @author arik
 */
public final class EventProperty extends Pair<String, Object>
{
    @Nonnull
    public static EventProperty eventProperty( @Nonnull String name, @Nullable Object value )
    {
        return new EventProperty( name, value );
    }

    private EventProperty( @Nonnull String left, @Nullable Object right )
    {
        super( left, right );
    }
}
