package org.mosaic.event;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface EventManager
{
    @Nonnull
    Event createEvent( @Nonnull String topic );

    void postEvent( @Nonnull Event event );

    void postEvent( @Nonnull Event event, @Nonnull EventDeliveryCallback callback );
}
