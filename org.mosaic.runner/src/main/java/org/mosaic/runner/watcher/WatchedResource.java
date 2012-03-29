package org.mosaic.runner.watcher;

import org.osgi.framework.BundleException;

/**
 * @author arik
 */
public interface WatchedResource {

    ScanResult check();

}
