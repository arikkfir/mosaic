package org.mosaic.server.web.dispatcher;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface RequestDispatcher {

    void handle( HttpRequest request );

}
