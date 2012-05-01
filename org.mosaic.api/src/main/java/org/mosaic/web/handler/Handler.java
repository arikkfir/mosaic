package org.mosaic.web.handler;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface Handler {

    interface HandlerMatch {

    }

    HandlerMatch matches( HttpRequest request );

    Object handle( HttpRequest request, HandlerMatch match ) throws Exception;

}
