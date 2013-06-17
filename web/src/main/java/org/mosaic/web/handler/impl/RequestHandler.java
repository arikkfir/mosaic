package org.mosaic.web.handler.impl;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.web.net.HttpStatus;

/**
 * @author arik
 */
@Bean
public class RequestHandler extends AbstractHandler
{
    // TODO arik: set "org.eclipse.jetty.servlet.SessionCookie" on jetty context
    // TODO arik: set "org.eclipse.jetty.servlet.SessionIdPathParameterName=none" on jetty context
    // TODO arik: set "org.eclipse.jetty.servlet.MaxAge=XXX" on jetty context
    // TODO arik: call "servletContextHandler.getSessionHandler().setSessionManager(...)"
    // TODO arik: use ResourceHandler for static content of web applications

    @Override
    public void handle( @Nonnull String target,
                        @Nonnull Request request,
                        @Nonnull HttpServletRequest httpServletRequest,
                        @Nonnull HttpServletResponse httpServletResponse ) throws IOException, ServletException
    {
        Response response = request.getResponse();

        // TODO arik: implement handle([target, request, httpServletRequest, httpServletResponse])
        response.setStatus( HttpStatus.OK.value() );
        response.getWriter().print( "Served!?" );

        request.setHandled( true );
    }
}
