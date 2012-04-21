package org.mosaic.server.web.jetty.impl;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.web.HttpApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;

/**
 * @author arik
 */
@Component
public class HttpApplicationJettyHandler extends ContextHandlerCollection {

    private Map<HttpApplication, ServletContextHandler> applications = new ConcurrentHashMap<>( 10 );

    private ConversionService conversionService;

    @Autowired
    public void setConversionService( ConversionService conversionService ) {
        this.conversionService = conversionService;
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
            HttpApplicationServlet servlet = new HttpApplicationServlet( application, this.conversionService );
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
}
