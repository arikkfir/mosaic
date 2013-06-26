package org.mosaic.web.handler.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.security.UserManager;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.request.WebResponse;
import org.mosaic.web.request.impl.UnsupportedHttpMethodException;
import org.mosaic.web.request.impl.WebRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class RequestHandler extends ContextHandlerCollection
{
    private static final Logger LOG = LoggerFactory.getLogger( RequestHandler.class );

    @Nonnull
    private final LoadingCache<Pair<String, String>, MapEx<String, String>> pathTemplatesCache;

    @Nonnull
    private Server server;

    @Nonnull
    private ConversionService conversionService;

    @Nonnull
    private UserManager userManager;

    @Nonnull
    private PathParametersCompiler pathParametersCompiler;

    @Nonnull
    private ExecutionPlanFactory executionPlanFactory;

    public RequestHandler()
    {
        this.pathTemplatesCache = CacheBuilder
                .newBuilder()
                .concurrencyLevel( 500 )
                .expireAfterAccess( 5, TimeUnit.MINUTES )
                .initialCapacity( 10000 )
                .maximumSize( 100000 )
                .build( new CacheLoader<Pair<String, String>, MapEx<String, String>>()
                {
                    @Override
                    public MapEx<String, String> load( @Nonnull Pair<String, String> key )
                            throws Exception
                    {
                        return RequestHandler.this.pathParametersCompiler.load( key );
                    }
                } );
    }

    @BeanRef
    public void setExecutionPlanFactory( @Nonnull ExecutionPlanFactory executionPlanFactory )
    {
        this.executionPlanFactory = executionPlanFactory;
    }

    @BeanRef
    public void setPathParametersCompiler( @Nonnull PathParametersCompiler pathParametersCompiler )
    {
        this.pathParametersCompiler = pathParametersCompiler;
    }

    @ServiceRef
    public void setServer( @Nonnull Server server )
    {
        this.server = server;
    }

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @ServiceRef
    public void setUserManager( @Nonnull UserManager userManager )
    {
        this.userManager = userManager;
    }

    @ServiceBind(updates = true)
    public void addWebApplication( @Nonnull WebApplication application )
    {
        // since this invocation might mean that the app was simply updated - lets completely remove it and add it a-new
        removeWebApplication( application );
        addHandler( new WebApplicationContextHandler( application ) );
    }

    @ServiceUnbind
    public void removeWebApplication( @Nonnull WebApplication application )
    {
        Handler[] handlers = getHandlers();
        if( handlers != null )
        {
            for( Handler handler : handlers )
            {
                if( handler instanceof WebApplicationContextHandler )
                {
                    WebApplicationContextHandler webApplicationContextHandler = ( WebApplicationContextHandler ) handler;
                    if( webApplicationContextHandler.application == application )
                    {
                        removeHandler( handler );
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void handle( String target,
                        Request baseRequest,
                        HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse ) throws IOException, ServletException
    {
        super.handle( target, baseRequest, httpServletRequest, httpServletResponse );
        if( !baseRequest.isHandled() )
        {
            try
            {
                WebResponse response = createWebRequest( baseRequest, UnknownHostWebApplication.INSTANCE ).getResponse();
                response.setStatus( HttpStatus.NOT_FOUND );
                response.disableCaching();
                baseRequest.setHandled( true );
            }
            catch( UnsupportedHttpMethodException e )
            {
                httpServletResponse.setStatus( HttpServletResponse.SC_NOT_IMPLEMENTED );
            }
        }
    }

    @Nonnull
    private WebRequestImpl createWebRequest( @Nonnull Request request, @Nonnull WebApplication webApplication )
            throws UnsupportedHttpMethodException
    {
        WebRequestImpl webRequest = new WebRequestImpl( request,
                                                        RequestHandler.this.conversionService,
                                                        webApplication,
                                                        RequestHandler.this.userManager,
                                                        RequestHandler.this.pathTemplatesCache );
        webRequest.getResponse().getHeaders().setServer( "Mosaic Web Server/" + RequestHandler.this.server.getVersion() );
        return webRequest;
    }

    private class WebApplicationContextHandler extends ServletContextHandler
    {
        @Nonnull
        private final org.mosaic.web.application.WebApplication application;

        private WebApplicationContextHandler( @Nonnull WebApplication application )
        {
            super( SESSIONS );

            this.application = application;
            this.setAllowNullPathInfo( false );
            this.setBaseResource( getApplicationContentRoots() );
            this.setCompactPath( true );
            this.setContextPath( "/" );
            this.setDisplayName( this.application.getDisplayName() );
            for( Map.Entry<String, String> entry : this.application.getParameters().entrySet() )
            {
                this.setInitParameter( entry.getKey(), entry.getValue() );
            }
            this.setInitParameter( SessionManager.__SessionIdPathParameterNameProperty, "none" );
            this.setInitParameter( SessionManager.__SessionCookieProperty, application.getName() + "_sessionid" );
            this.setInitParameter( SessionManager.__MaxAgeProperty, application.getMaxSessionAge().toStandardSeconds().getSeconds() + "" );
            this.setErrorHandler( new ErrorHandler() );
            this.setLogger( Log.getLogger( "org.mosaic.web.application" ) );
            this.setServletHandler( new ServletHandler() );
            this.setVirtualHosts( getApplicationVirtualHosts() );
            this.addServlet( new ServletHolder( "dispatcher", new WebApplicationServlet() ), "/" );
        }

        private ResourceCollection getApplicationContentRoots()
        {
            ResourceCollection baseResources = new ResourceCollection();
            for( Path contentRoot : this.application.getContentRoots() )
            {
                try
                {
                    baseResources.addPath( contentRoot.toString() );
                }
                catch( IOException e )
                {
                    LOG.warn( "Could not add web application content root '{}': {}", contentRoot, e.getMessage(), e );
                }
            }
            return baseResources;
        }

        private String[] getApplicationVirtualHosts()
        {
            Set<String> virtualHosts = this.application.getVirtualHosts();
            return virtualHosts.toArray( new String[ virtualHosts.size() ] );
        }

        private class WebApplicationServlet extends HttpServlet
        {
            @Override
            protected void doGet( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                handleRequest( req, resp );
            }

            @Override
            protected void doPost( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                handleRequest( req, resp );
            }

            @Override
            protected void doPut( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                handleRequest( req, resp );
            }

            @Override
            protected void doDelete( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                handleRequest( req, resp );
            }

            @Override
            protected void doOptions( HttpServletRequest req, HttpServletResponse resp )
                    throws ServletException, IOException
            {
                handleRequest( req, resp );
            }

            protected void handleRequest( @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp )
            {
                WebRequestImpl request;
                try
                {
                    request = createWebRequest( ( Request ) req, application );
                }
                catch( UnsupportedHttpMethodException e )
                {
                    resp.setStatus( HttpServletResponse.SC_NOT_IMPLEMENTED );
                    return;
                }
                catch( Throwable e )
                {
                    LOG.error( "Unexpected error while building web request: {}", e.getMessage(), e );
                    resp.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                    return;
                }

                try
                {
                    request.getResponse().setStatus( HttpStatus.OK );
                    request.getResponse().disableCaching();

                    if( "/favicon.ico".equalsIgnoreCase( request.getUri().getEncodedPath() ) )
                    {
                        // TODO arik: allow standard favicon resource serving
                        request.getResponse().setStatus( HttpStatus.NOT_FOUND );
                        return;
                    }

                    executionPlanFactory.buildExecutionPlan( request ).execute();
                }
                catch( Throwable e )
                {
                    request.dumpToLog( "Unexpected error during request handling: {}", e.getMessage(), e );
                    resp.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                }
            }
        }
    }
}
