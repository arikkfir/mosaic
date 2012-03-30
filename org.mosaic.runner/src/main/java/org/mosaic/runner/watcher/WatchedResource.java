package org.mosaic.runner.watcher;

import java.io.IOException;
import org.osgi.framework.BundleException;

/**
 * @author arik
 */
public interface WatchedResource {

    ScanResult check() throws IOException, BundleException;

    ScanResult uninstall() throws IOException, BundleException;

}
