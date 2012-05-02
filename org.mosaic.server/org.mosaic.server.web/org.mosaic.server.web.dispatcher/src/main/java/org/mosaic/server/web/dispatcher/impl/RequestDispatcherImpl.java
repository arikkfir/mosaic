package org.mosaic.server.web.dispatcher.impl;

import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.server.web.dispatcher.impl.handler.HandlersManager;
import org.mosaic.server.web.dispatcher.impl.handler.RequestExecutionPlan;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( RequestDispatcher.class )
public class RequestDispatcherImpl implements RequestDispatcher
{

    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcherImpl.class );

    private HandlersManager handlersManager;

    @Autowired
    public void setHandlersManager( HandlersManager handlersManager )
    {
        this.handlersManager = handlersManager;
    }

    @Override
    @Trace
    public void handle( HttpRequest request )
    {
        RequestExecutionPlan plan = new RequestExecutionPlan( request );
        this.handlersManager.applyHandler( plan );
        //TODO 5/2/12: interceptorsManager.applyInterceptors(plan)
    }

}
