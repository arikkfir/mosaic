package org.mosaic.server.web.dispatcher.impl;

import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.util.Http;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( RequestDispatcher.class )
public class RequestDispatcherImpl implements RequestDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcherImpl.class );

    @Override
    public void handle() {
        HttpRequest request = Http.requireRequest();
        System.out.println( "Handling: " + request.getUrl() );
    }

}
