package org.mosaic.admin.impl;

import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.annotation.Controller;
import org.mosaic.web.handler.annotation.Get;
import org.mosaic.web.handler.annotation.Service;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class AdminAgent {

    private static final Logger LOG = LoggerFactory.getLogger( AdminAgent.class );

    @Get
    @Service( "/services/arik" )
    @Trace
    public void myService( HttpRequest request ) {
    }

    @Get
    @Controller( "/controllers/arik" )
    @Trace
    public void myController( HttpRequest request ) {
    }

}
