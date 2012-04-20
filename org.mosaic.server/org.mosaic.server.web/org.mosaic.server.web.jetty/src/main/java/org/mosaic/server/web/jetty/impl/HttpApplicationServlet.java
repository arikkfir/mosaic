package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mosaic.web.HttpApplication;

/**
 * @author arik
 */
public class HttpApplicationServlet extends HttpServlet {

    private final HttpApplication httpApplication;

    public HttpApplicationServlet( HttpApplication httpApplication ) {
        this.httpApplication = httpApplication;
    }

    @Override
    public void service( HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        System.out.println( "Hello3!" );
        super.service( request, response );
    }

}
