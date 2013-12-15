package org.mosaic.web.impl;

import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.modules.Service;
import org.mosaic.web.application.Application;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.handler.impl.RequestHandlerManager;
import org.mosaic.web.request.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.mosaic.web.request.impl.WebRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Component
final class RequestDispatcher extends HttpServlet
{
    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcher.class );

    @Nonnull
    @Component
    private Module module;

    @Nonnull
    @Service
    private List<Application> applications;

    @Nonnull
    @Component
    private RequestHandlerManager requestHandlerManager;

    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        resp.setHeader( HttpHeaders.SERVER, "Mosaic Web Server/" + this.module.getContext().getServerVersion() );

        Application application = findApplication( req );
        if( application == null )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        WebRequest request = new WebRequestImpl( application, ( Request ) req );

        RequestHandler requestHandler = this.requestHandlerManager.findRequestHandler( request );
        if( requestHandler == null )
        {
            // TODO: add application-specific not-found handling, maybe error page, etc
            WebResponse response = request.getResponse();
            response.setStatus( HttpStatus.NOT_FOUND );
            response.disableCaching();
            return;
        }

        try
        {
            requestHandler.handle( request );
        }
        catch( Throwable throwable )
        {
            // TODO: add application-specific error handling, maybe error page, etc
            WebResponse response = request.getResponse();
            response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
            response.disableCaching();
            request.dumpToErrorLog( LOG, "Request handler '{}' failed: {}", throwable.getMessage(), throwable );
        }
    }

    @Nullable
    private Application findApplication( @Nonnull HttpServletRequest request )
    {
        for( Application application : this.applications )
        {
            if( application.getVirtualHosts().contains( request.getServerName().toLowerCase() ) )
            {
                return application;
            }
        }
        return null;
    }
}
