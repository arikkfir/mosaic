package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.web.Http;
import org.mosaic.web.HttpApplication;
import org.springframework.core.convert.ConversionService;

/**
 * @author arik
 */
public class HttpApplicationServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger( HttpApplicationServlet.class );

    private final HttpApplication httpApplication;

    private final ConversionService conversionService;

    public HttpApplicationServlet( HttpApplication httpApplication, ConversionService conversionService ) {
        this.httpApplication = httpApplication;
        this.conversionService = conversionService;
    }

    @Override
    public void service( HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {

        try {

            // associate app and request on the thread
            Http.setApplication( this.httpApplication );
            Http.setRequest( new HttpRequestImpl( this.conversionService, ( Request ) request, response ) );

            // handle request
            super.service( request, response );

        } catch( @SuppressWarnings( "CaughtExceptionImmediatelyRethrown" ) IOException | ServletException e ) {

            // bubble up I/O or servlet exceptions
            throw e;

        } catch( Exception e ) {

            // log and return 500
            LOG.error( "Error processing request '{}': {}", request.getRequestURL(), e.getMessage(), e );
            if( !response.isCommitted() ) {
                response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            }

        } finally {
            Http.setRequest( null );
            Http.setApplication( null );
        }
    }

}
