package org.mosaic.server.web.dispatcher.impl;

import java.util.LinkedList;
import java.util.List;
import org.mosaic.lifecycle.ServiceExport;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.server.web.dispatcher.impl.handler.ExceptionHandlersManager;
import org.mosaic.server.web.dispatcher.impl.handler.MarshallersManager;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.util.logging.Trace;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Marshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
@ServiceExport( RequestDispatcher.class )
public class RequestDispatcherImpl implements RequestDispatcher
{
    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcherImpl.class );

    private List<RequestExecutionPlan.RequestExecutionBuilder> planBuilders;

    private MarshallersManager marshallersManager;

    private ExceptionHandlersManager exceptionHandlersManager;

    @Autowired
    public void setMarshallersManager( MarshallersManager marshallersManager )
    {
        this.marshallersManager = marshallersManager;
    }

    @Autowired
    public void setExceptionHandlersManager( ExceptionHandlersManager exceptionHandlersManager )
    {
        this.exceptionHandlersManager = exceptionHandlersManager;
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
            // execute the request
            handlerResult = buildExecutionPlan( request ).execute();
        }
        catch( Exception e )
        {
            // handle the exception
            handlerResult = this.exceptionHandlersManager.handleException( request, e );
        }

        if( handlerResult != null )
        {
            try
            {
                // marshall the response
                marshallResult( request, handlerResult );
            }
            catch( Exception e )
            {
                sendErrorToClient( request, e );
            }
        }
    }

    private void marshallResult( HttpRequest request, Object result ) throws Exception
    {
        List<Marshaller> invokedMarshallers = new LinkedList<>();
        while( result != null )
        {
            Marshaller marshaller = this.marshallersManager.getMarshaller( request, result );
            if( marshaller == null )
            {
                sendNotAcceptableErrorToClient( request );
                break;
            }
            else if( invokedMarshallers.contains( marshaller ) )
            {
                throw new IllegalStateException( "Cyclic marshaller invocation: " + invokedMarshallers );
            }
            else
            {
                invokedMarshallers.add( marshaller );
                result = marshaller.marshall( request, result );
            }
        }
    }

    private void sendNotAcceptableErrorToClient( HttpRequest request )
    {
        if( request.isCommitted() )
        {
            LOG.warn( "Could not send HTTP 406 (Not Acceptable) error to client because the request has already been committed" );
        }
        else
        {
            request.setResponseStatus( HttpStatus.NOT_ACCEPTABLE, "Unsupported 'Accept' header values" );
        }
    }

    private void sendErrorToClient( HttpRequest request, Exception exception )
    {
        LOG.error( "Request processing error: {}", exception.getMessage(), exception );
        if( request.isCommitted() )
        {
            LOG.warn( "Could not send HTTP error to client (due to previous error) because the request has already been committed" );
        }
        else
        {
            request.setResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR, "An internal error has occurred" );
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
