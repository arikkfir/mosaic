package org.mosaic.server.web.dispatcher.impl;

import java.util.List;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
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
    private List<RequestExecutionPlan.RequestExecutionBuilder> planBuilders;

    @Autowired
    public void setPlanBuilders( List<RequestExecutionPlan.RequestExecutionBuilder> planBuilders )
    {
        this.planBuilders = planBuilders;
    }

    @Override
    @Trace
    public void handle( HttpRequest request )
    {
        RequestExecutionPlan plan = buildExecutionPlan( request );
        try
        {
            plan.execute();
        }
        catch( Exception e )
        {
            //TODO: handle error by invoking @ExceptionHandler(s)
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private RequestExecutionPlan buildExecutionPlan( HttpRequest request )
    {
        RequestExecutionPlan plan = new RequestExecutionPlan( request );
        request.put( RequestExecutionPlan.class.getName(), plan );

        for( RequestExecutionPlan.RequestExecutionBuilder builder : this.planBuilders )
        {
            builder.contribute( plan );
        }
        return plan;
    }

}
