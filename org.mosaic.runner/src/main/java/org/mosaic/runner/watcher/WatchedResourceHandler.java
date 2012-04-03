package org.mosaic.runner.watcher;

import java.nio.file.Path;

/**
 * @author arik
 */
public interface WatchedResourceHandler {

    void handleNoLongerExists( Path resource ) throws Exception;

    void handleIllegalFile( Path resource ) throws Exception;

    Long getLastUpdateTime( Path resource ) throws Exception;

    void handleUpdated( Path resource ) throws Exception;

    void handleUpToDate( Path resource ) throws Exception;

}
