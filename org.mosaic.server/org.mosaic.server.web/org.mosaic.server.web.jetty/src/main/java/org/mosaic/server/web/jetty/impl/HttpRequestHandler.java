package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.web.dispatcher.RequestDispatcher;
import org.mosaic.web.HttpApplication;
import org.mosaic.web.util.Http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;

/**
 * @author arik
 */
@Component
public class HttpRequestHandler extends ContextHandlerCollection {

    private static final Logger LOG = LoggerFactory.getLogger( HttpRequestHandler.class );

    private Map<HttpApplication, ServletContextHandler> applications = new ConcurrentHashMap<>( 10 );

    private ConversionService conversionService;

    private RequestDispatcher dispatcher;

    @Autowired
    public void setConversionService( ConversionService conversionService ) {
        this.conversionService = conversionService;
    }

    @ServiceRef( required = false )
    public void setDispatcher( RequestDispatcher dispatcher ) {
        this.dispatcher = dispatcher;
    }

    @ServiceBind
    public void addApplication( HttpApplication application ) {
        ServletContextHandler context = this.applications.get( application );
        if( context == null ) {

            context = new ServletContextHandler( SESSIONS );
            context.setAliases( false );
            context.setAllowNullPathInfo( true );
            context.setCompactPath( true );
            context.setContextPath( "" );
            context.setLogger( new Slf4jLog( ServletContextHandler.class.getName() ) );
            context.setErrorHandler( new ErrorHandler() );

            // add filters
            context.setDisplayName( application.getName() );
            context.addFilter( new FilterHolder( new GzipFilter() ), "/*", EnumSet.of( DispatcherType.REQUEST ) );

            // add application filters and servlet
            HttpApplicationServlet servlet = new HttpApplicationServlet( application );
            context.addServlet( new ServletHolder( "MosaicServlet", servlet ), "/*" );

            // update virtual hosts
            Set<String> virtualHosts = application.getVirtualHosts();
            context.setVirtualHosts( virtualHosts.toArray( new String[ virtualHosts.size() ] ) );

            // add application
            this.applications.put( application, context );
            addHandler( context );

        } else {

            // update virtual hosts
            Set<String> virtualHosts = application.getVirtualHosts();
            context.setVirtualHosts( virtualHosts.toArray( new String[ virtualHosts.size() ] ) );

            // update path mappings
            mapContexts();

        }
    }

    @ServiceUnbind
    public void removeApplication( HttpApplication application ) {
        ServletContextHandler context = this.applications.remove( application );
        if( context != null ) {
            removeHandler( context );
        }
    }

    public class HttpApplicationServlet extends HttpServlet {

        private final HttpApplication httpApplication;

        public HttpApplicationServlet( HttpApplication httpApplication ) {
            this.httpApplication = httpApplication;
        }

        @Override
        protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            handleRequest( req, resp );
        }

        @Override
        protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            handleRequest( req, resp );
        }

        @Override
        protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            handleRequest( req, resp );
        }

        @Override
        protected void doDelete( HttpServletRequest req, HttpServletResponse resp )
                throws ServletException, IOException {
            handleRequest( req, resp );
        }

        @Override
        protected void doOptions( HttpServletRequest req, HttpServletResponse resp )
                throws ServletException, IOException {
            handleRequest( req, resp );
        }

        private void handleRequest( HttpServletRequest request, HttpServletResponse response )
                throws IOException, ServletException {

            try {

                // associate app and request on the thread
                Http.setApplication( this.httpApplication );
                Http.setRequest( new HttpRequestImpl( conversionService, request, response ) );

                // if no dispatcher, send 404; otherwise, pass the torch to the dispatcher
                RequestDispatcher dispatcher = HttpRequestHandler.this.dispatcher;
                if( dispatcher == null ) {
                    response.sendError( HttpServletResponse.SC_NOT_FOUND );
                } else {
                    dispatcher.handle();
                }

            } catch( @SuppressWarnings( "CaughtExceptionImmediatelyRethrown" ) IOException | ServletException e ) {

                // bubble up I/O or servlet exceptions
                throw e;

            } catch( Exception e ) {

                // log and return 500
                LOG.error( "Error processing request '{}': {}", request.getRequestURL(), e.getMessage(), e );
                response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );

            } finally {
                Http.setRequest( null );
                Http.setApplication( null );
            }
        }

    }
}