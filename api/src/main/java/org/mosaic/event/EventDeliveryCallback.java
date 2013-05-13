package org.mosaic.event;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface EventDeliveryCallback
{
    void eventDelivered( @Nonnull Event event );
}
