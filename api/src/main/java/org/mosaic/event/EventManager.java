package org.mosaic.event;

import javax.annotation.Nonnull;
import org.mosaic.util.pair.Pair;

/**
 * @author arik
 */
public interface EventManager
{
    @Nonnull
    Event createEvent( @Nonnull String topic, @Nonnull Pair<String, ?>... propertyPairs );

    void postEvent( @Nonnull Event event );

    void postEvent( @Nonnull Event event, @Nonnull EventDeliveryCallback callback );
}
