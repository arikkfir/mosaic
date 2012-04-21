package org.mosaic.server.web.dispatcher.impl;

import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.web.Http;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( RequestDispatcher.class )
public class RequestDispatcherImpl implements RequestDispatcher {

    @Override
    public void handle() {
        System.out.println( "Handling: " + Http.request().getUrl() );
    }

}
