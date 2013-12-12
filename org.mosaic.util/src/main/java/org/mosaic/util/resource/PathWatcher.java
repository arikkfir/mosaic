package org.mosaic.util.resource;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface PathWatcher
{
    void handle( @Nonnull PathWatcherContext context ) throws Exception;
}
