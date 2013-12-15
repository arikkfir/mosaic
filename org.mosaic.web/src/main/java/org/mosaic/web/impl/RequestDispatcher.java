package org.mosaic.web.impl;

import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

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
        if( request.getMethod().equals( "OPTIONS" ) )
        {
            List<RequestHandler> handlers = this.requestHandlerManager.findRequestHandlers( request, Integer.MAX_VALUE );
            if( handlers.isEmpty() )
            {
                request.getResponse().getHeaders().setAllow( singletonList( "OPTIONS" ) );
            }
            else
            {
                Set<String> httpMethods = Sets.newHashSet( "OPTIONS" );
                for( RequestHandler handler : handlers )
                {
                    Set<String> handlerHttpMethods = handler.getHttpMethods();
                    if( handlerHttpMethods == null )
                    {
                        httpMethods.addAll( asList( "GET", "HEAD" ) );
                    }
                    else
                    {
                        httpMethods.addAll( handlerHttpMethods );
                    }
                }
                request.getResponse().getHeaders().setAllow( new LinkedList<>( httpMethods ) );
            }
        }
        else
        {
            List<RequestHandler> requestHandlers = this.requestHandlerManager.findRequestHandlers( request, 1 );
            if( requestHandlers.isEmpty() )
            {
                // TODO: add application-specific not-found handling, maybe error page, etc
                WebResponse response = request.getResponse();
                response.setStatus( HttpStatus.NOT_FOUND );
                response.disableCaching();
                return;
            }

            RequestHandler requestHandler = requestHandlers.get( 0 );
            try
            {
                requestHandler.handle( request );
            }
            catch( Throwable throwable )
            {
                // TODO: add application-specific error handling, maybe error page, @ErrorHandler(s), etc
                WebResponse response = request.getResponse();
                response.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
                response.disableCaching();
                request.dumpToErrorLog( LOG, "Request handler '{}' failed: {}", throwable.getMessage(), throwable );
            }
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
