package org.mosaic.web.handler.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.joda.time.Period;
import org.mosaic.Server;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.security.UserManager;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.handler.impl.util.PathParametersCompiler;
import org.mosaic.web.net.HttpStatus;
import org.mosaic.web.request.WebRequest;
import org.mosaic.web.request.WebResponse;
import org.mosaic.web.request.impl.UnsupportedHttpMethodException;
import org.mosaic.web.request.impl.WebRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.web.net.HttpMethod.GET;

/**
 * @author arik
 */
@Bean
public class RequestDispatcher extends ContextHandlerCollection
{
    private static final Logger LOG = LoggerFactory.getLogger( RequestDispatcher.class );

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
    private WebEndpointsManager webEndpointsManager;

    public RequestDispatcher()
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
                        return RequestDispatcher.this.pathParametersCompiler.load( key );
                    }
                } );
    }

    @BeanRef
    public void setWebEndpointsManager( @Nonnull WebEndpointsManager webEndpointsManager )
    {
        this.webEndpointsManager = webEndpointsManager;
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

    @ServiceBind( updates = true )
    public void addWebApplication( @Nonnull WebApplication application ) throws IOException, URISyntaxException
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
    private WebRequest createWebRequest( @Nonnull Request request, @Nonnull WebApplication webApplication )
            throws UnsupportedHttpMethodException
    {
        WebRequestImpl webRequest = new WebRequestImpl( request,
                                                        RequestDispatcher.this.conversionService,
                                                        webApplication,
                                                        RequestDispatcher.this.userManager,
                                                        RequestDispatcher.this.pathTemplatesCache );
        webRequest.getResponse().getHeaders().setServer( "Mosaic Web Server/" + RequestDispatcher.this.server.getVersion() );
        return webRequest;
    }

    private class WebApplicationContextHandler extends ServletContextHandler
    {
        @Nonnull
        private final org.mosaic.web.application.WebApplication application;

        private WebApplicationContextHandler( @Nonnull WebApplication application )
                throws IOException, URISyntaxException
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
            this.setVirtualHosts( getApplicationVirtualHosts() );

            ServletHolder servletHolder = new ServletHolder( "dispatcher", new WebApplicationServlet() );
            servletHolder.setInitParameter( "etags", "true" );
            servletHolder.setInitParameter( "maxCachedFiles", 50000 + "" );
            servletHolder.setInitParameter( "maxCacheSize", 1024 * 1024 * 50 + "" );
            servletHolder.setInitParameter( "maxCachedFileSize", 1024 * 1024 * 1 + "" );
            this.addServlet( servletHolder, "/" );
        }

        private ResourceCollection getApplicationContentRoots() throws IOException, URISyntaxException
        {
            ResourceCollection baseResources = new ResourceCollection();

            Collection<Path> contentRoots = this.application.getWebContent().getContentRoots();
            Path[] contentRootsArr = contentRoots.toArray( new Path[ contentRoots.size() ] );
            Resource[] contentRootResources = new Resource[ contentRootsArr.length ];
            for( int i = 0; i < contentRootsArr.length; i++ )
            {
                Path path = contentRootsArr[ i ];
                contentRootResources[ i ] = new FileResource( path.toUri().toURL() );
            }
            baseResources.setResources( contentRootResources );
            return baseResources;
        }

        private String[] getApplicationVirtualHosts()
        {
            Set<String> virtualHosts = this.application.getVirtualHosts();
            return virtualHosts.toArray( new String[ virtualHosts.size() ] );
        }

        private class WebApplicationServlet extends DefaultServlet
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
                WebRequest request;
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

                    if( request.getMethod() == GET && "/jetty-dir.css".equals( request.getUri().getDecodedPath() ) )
                    {
                        URL jettyDirUrl = getClass().getClassLoader().getResource( "jetty-dir.css" );
                        if( jettyDirUrl != null )
                        {
                            request.getResponse().allowPublicCaches( Period.hours( 1 ) );
                            request.getResponse().setStatus( HttpStatus.OK );
                            Resources.copy( jettyDirUrl, request.getResponse().getBinaryBody() );
                            return;
                        }
                    }

                    Period cachePeriod = request.getApplication().getWebContent().getCachePeriod( request.getUri().getEncodedPath() );
                    if( cachePeriod != null )
                    {
                        if( cachePeriod.toStandardSeconds().getSeconds() == 0 )
                        {
                            request.getResponse().disableCaching();
                        }
                        else
                        {
                            request.getResponse().allowPublicCaches( cachePeriod );
                        }
                    }

                    RequestExecutionPlan plan = webEndpointsManager.createRequestExecutionPlan( request );
                    if( plan.canHandle() )
                    {
                        plan.execute();
                    }
                    else
                    {
                        switch( request.getMethod() )
                        {
                            case GET:
                                super.doGet( req, resp );
                                return;
                            case POST:
                                super.doPost( req, resp );
                                return;
                            case OPTIONS:
                                super.doOptions( req, resp );
                                return;
                            case TRACE:
                                super.doTrace( req, resp );
                                return;
                            default:
                                if( request.getProtocol().endsWith( "1.1" ) )
                                {
                                    request.getResponse().setStatus( HttpStatus.METHOD_NOT_ALLOWED );
                                }
                                else
                                {
                                    request.getResponse().setStatus( HttpStatus.BAD_REQUEST );
                                }
                        }
                    }
                }
                catch( Throwable e )
                {
                    request.dumpToErrorLog( "Unexpected error during request handling: {}", e.getMessage(), e );
                    resp.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
                }
            }
        }
    }
}
