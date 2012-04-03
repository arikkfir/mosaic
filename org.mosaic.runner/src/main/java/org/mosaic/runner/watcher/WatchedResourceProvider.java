package org.mosaic.runner.watcher;

import java.util.Collection;

/**
 * @author arik
 */
public interface WatchedResourceProvider {

    Collection<WatchedResource> getWatchedResources();

}
