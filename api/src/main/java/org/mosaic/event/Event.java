package org.mosaic.event;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;

/**
 * @author arik
 */
public interface Event
{
    @Nonnull
    String getTopic();

    @Nonnull
    MapEx<String, Object> getProperties();
}
