package org.mosaic.web.server.impl;

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
import org.mosaic.modules.Service;
import org.mosaic.server.Server;
import org.mosaic.web.application.Application;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
final class RequestDispatcher extends HttpServlet
{
    @Nonnull
    @Service
    private Server server;

    @Nonnull
    @Service
    private List<Application> applications;

    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        resp.setHeader( HttpHeaders.SERVER, "Mosaic Web Server/" + this.server.getVersion() );

        Application application = findApplication( req );
        if( application == null )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        WebInvocation request = new WebInvocationImpl( ( Request ) req, application );
        Runnable plan = new RequestPlan( request );
        plan.run();
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
