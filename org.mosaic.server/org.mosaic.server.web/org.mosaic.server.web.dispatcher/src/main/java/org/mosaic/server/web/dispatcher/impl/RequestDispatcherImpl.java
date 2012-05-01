package org.mosaic.server.web.dispatcher.impl;

import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.Http;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( RequestDispatcher.class )
public class RequestDispatcherImpl implements RequestDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcherImpl.class );

    @Override
    @Trace
    public void handle() {
        HttpRequest request = Http.requireRequest();
        System.out.println( "Handling: " + request.getUrl() );
    }

}
