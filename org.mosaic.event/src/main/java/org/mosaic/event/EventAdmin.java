package org.mosaic.event;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface EventAdmin
{
    void postEvent( @Nonnull Object event, @Nonnull EventProperty... properties );

    void sendEvent( @Nonnull Object event, @Nonnull EventProperty... properties );
}
