package org.mosaic.server.web.dispatcher.impl;

import java.util.LinkedList;
import java.util.List;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.server.web.dispatcher.impl.handler.MarshallersManager;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Marshaller;
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

    private MarshallersManager marshallersManager;

    @Autowired
    public void setMarshallersManager( MarshallersManager marshallersManager )
    {
        this.marshallersManager = marshallersManager;
    }

    @Autowired
    public void setPlanBuilders( List<RequestExecutionPlan.RequestExecutionBuilder> planBuilders )
    {
        this.planBuilders = planBuilders;
    }

    @Override
    @Trace
    public void handle( HttpRequest request )
    {
        Object handlerResult;
        try
        {
            handlerResult = buildExecutionPlan( request ).execute();
        }
        catch( Exception e )
        {
            //TODO: handle error by invoking @ExceptionHandler(s)
            handlerResult = null;// TODO by arik on 5/4/12: return result from exception handler if any (null otherwise)
        }

        if( handlerResult != null )
        {
            marshallResult( request, handlerResult );
        }
        else
        {
            // TODO by arik on 5/4/12: log this
            // handler took care of sending a response to the client - no need to marshall a response
        }
    }

    private void marshallResult( HttpRequest request, Object result )
    {
        List<Marshaller> invokedMarshallers = new LinkedList<>();
        while( result != null )
        {
            Marshaller marshaller = this.marshallersManager.getMarshaller( request, result );
            if( marshaller == null )
            {
                // TODO by arik on 5/4/12: log this and send error to client
                return;
            }
            else if( invokedMarshallers.contains( marshaller ) )
            {
                // TODO by arik on 5/4/12: log this and send error to client
                return;
            }
            else
            {
                invokedMarshallers.add( marshaller );
                try
                {
                    result = marshaller.marshall( request, result );
                }
                catch( Exception e )
                {
                    // TODO by arik on 5/4/12: log this and return error to client if request not committed
                    return;
                }
            }
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
