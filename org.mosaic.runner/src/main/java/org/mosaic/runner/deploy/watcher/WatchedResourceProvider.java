package org.mosaic.runner.deploy.watcher;

import java.util.Collection;

/**
 * @author arik
 */
public interface WatchedResourceProvider {

    Collection<WatchedResource> getWatchedResources();

}
