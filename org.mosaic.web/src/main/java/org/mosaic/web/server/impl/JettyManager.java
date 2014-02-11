package org.mosaic.web.server.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.MultiPartFilter;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.joda.time.Period;
import org.mosaic.config.Configurable;
import org.mosaic.modules.*;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_KEYMANAGERFACTORY_ALGORITHM;
import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM;

/**
 * @author arik
 */
@Component
final class JettyManager
{
    private static final Logger LOG = LoggerFactory.getLogger( JettyManager.class );

    private static final String[] EMPTY_STRINGS_ARRAY = new String[ 0 ];

    private static class NoErrorPageErrorHandler extends ErrorHandler
    {
        @Override
        protected void handleErrorPage( HttpServletRequest request, Writer writer, int code, String message )
                throws IOException
        {
            // no-op
        }
    }

    @Nonnull
    private final Set<Application> applications = new ConcurrentHashSet<>();

    @Nonnull
    private final RequestDispatcher requestDispatcher = new RequestDispatcher();

    @Nullable
    private org.eclipse.jetty.server.Server jettyServer;

    @Nonnull
    @Service
    private org.mosaic.server.Server mosaicServer;

    @Nullable
    private ContextHandlerCollection contextHandlerCollection;

    @Configurable("web")
    void configure( @Nonnull final MapEx<String, String> cfg )
    {
        LOG.info( "Web server configured - {}", this.jettyServer != null ? "restarting" : "starting" );
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                startJettyServer( cfg );
            }
        }, "StartWebServer" ).start();
    }

    @PreDestroy
    void destroy()
    {
        // stop the web server
        final Server server = this.jettyServer;
        if( server != null )
        {
            LOG.info( "Web module is deactivating" );
            new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    stopJettyServer( server );
                }
            }, "StopWebServer" ).start();
        }
    }

    @OnServiceAdded
    void onApplicationAdded( @Nonnull ServiceReference<Application> reference )
    {
        Application application = reference.service().get();
        this.applications.add( application );
        addContext( application );
    }

    @OnServiceRemoved
    void onApplicationRemoved( @Nonnull ServiceReference<Application> reference )
    {
        Application application = reference.service().get();
        removeContext( application );
        this.applications.remove( application );
    }

    private void addContext( @Nonnull Application application )
    {
        removeContext( application );

        ContextHandlerCollection contextHandlerCollection = this.contextHandlerCollection;
        if( contextHandlerCollection != null )
        {
            MapEx<String, String> appCtx = application.getContext();

            ServletContextHandler contextHandler = new ServletContextHandler( ServletContextHandler.SESSIONS );
            contextHandler.setAllowNullPathInfo( false );
            contextHandler.setAttribute( Application.class.getName(), application );
            contextHandler.setAttribute( "javax.servlet.context.tempdir", this.mosaicServer.getWorkPath().resolve( "web" ).resolve( application.getId() ).resolve( "temp" ).toFile() );
            contextHandler.setCompactPath( true );
            contextHandler.setDisplayName( application.getName() );
            contextHandler.setErrorHandler( new NoErrorPageErrorHandler() );
            contextHandler.setVirtualHosts( Iterables.toArray( application.getVirtualHosts(), String.class ) );

            FilterHolder crossOriginFilter = new FilterHolder( CrossOriginFilter.class );
            crossOriginFilter.setInitParameter( "allowedOrigins", appCtx.find( "crossOrigin.allowedOrigins" ).or( "bad://bad.com" ) );
            crossOriginFilter.setInitParameter( "allowedMethods", appCtx.get( "crossOrigin.allowedMethods" ) );
            crossOriginFilter.setInitParameter( "allowedHeaders", appCtx.get( "crossOrigin.allowedHeaders" ) );
            crossOriginFilter.setInitParameter( "preflightMaxAge", appCtx.get( "crossOrigin.preflightMaxAge" ) );
            crossOriginFilter.setInitParameter( "allowCredentials", appCtx.get( "crossOrigin.allowCredentials" ) );
            crossOriginFilter.setInitParameter( "exposeHeaders", appCtx.get( "crossOrigin.exposeHeaders" ) );
            crossOriginFilter.setInitParameter( "chainPreflight", appCtx.get( "crossOrigin.chainPreflight" ) );
            contextHandler.addFilter( crossOriginFilter, "/", EnumSet.of( DispatcherType.REQUEST ) );

            FilterHolder multipartFilter = new FilterHolder( MultiPartFilter.class );
            multipartFilter.setInitParameter( "delete", "true" );
            multipartFilter.setInitParameter( "deleteFiles", "true" );
            multipartFilter.setInitParameter( "maxFileSize", appCtx.find( "upload.maxFileSize" ).or( 1024 * 1000 * 5 + "" ) );
            multipartFilter.setInitParameter( "maxRequestSize", appCtx.get( "upload.maxRequestSize" ) );
            contextHandler.addFilter( multipartFilter, "/", EnumSet.of( DispatcherType.REQUEST ) );

            contextHandler.addServlet( new ServletHolder( "requestDispatcher", this.requestDispatcher ), "/" );

            Period maxSessionAge = application.getMaxSessionAge();
            int seconds = maxSessionAge.normalizedStandard().toStandardSeconds().getSeconds();
            contextHandler.setInitParameter( SessionManager.__MaxAgeProperty, seconds + "" );

            contextHandlerCollection.addHandler( contextHandler );
        }
    }

    private void removeContext( @Nonnull Application application )
    {
        ContextHandlerCollection contextHandlerCollection = this.contextHandlerCollection;
        if( contextHandlerCollection != null )
        {
            Handler[] handlers = contextHandlerCollection.getHandlers();
            if( handlers != null )
            {
                for( Handler handler : handlers )
                {
                    if( handler instanceof ContextHandler )
                    {
                        ContextHandler contextHandler = ( ContextHandler ) handler;
                        Object appAttr = contextHandler.getAttribute( Application.class.getName() );
                        if( appAttr instanceof Application )
                        {
                            Application app = ( Application ) appAttr;
                            if( app.getId().equals( application.getId() ) )
                            {
                                contextHandlerCollection.removeHandler( contextHandler );
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized void startJettyServer( @Nonnull MapEx<String, String> cfg )
    {
        Server server = this.jettyServer;

        // stop the web server
        if( server != null )
        {
            stopJettyServer( server );
        }

        // create and start the web server
        server = null;
        try
        {
            server = createServer( cfg );
            server.start();
            this.jettyServer = server;

            this.contextHandlerCollection = ( ContextHandlerCollection ) this.jettyServer.getHandler();

            for( Application application : this.applications )
            {
                addContext( application );
            }

            LOG.info( "Started web server" );
        }
        catch( Throwable e )
        {
            LOG.error( "Could not start web server: {}", e.getMessage(), e );
            if( server != null )
            {
                stopJettyServer( server );
            }
        }
    }

    private synchronized void stopJettyServer( @Nonnull Server server )
    {
        try
        {
            server.stop();
            LOG.info( "Stopped web server" );
        }
        catch( Throwable e )
        {
            LOG.warn( "Could not stop web server - {}", e.getMessage(), e );
        }
        finally
        {
            this.contextHandlerCollection = null;
            this.jettyServer = null;
        }
    }

    private Server createServer( MapEx<String, String> cfg ) throws Exception
    {
        Server server = new Server();
        server.setDumpAfterStart( false );
        server.setDumpBeforeStop( false );
        server.setStopAtShutdown( false ); // mosaic will close the module on jvm close anyway
        server.setStopTimeout( cfg.find( "stopTimeout", Long.class ).or( 60 * 1000l ) );
        server.setConnectors( createConnectors( server, cfg ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormContentSize", cfg.find( "maxFormContentSize", Integer.class ).or( 100000 ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormKeys", cfg.find( "maxFormKeys", Integer.class ).or( 2000 ) );
        server.setHandler( new ContextHandlerCollection() );
        return server;
    }

    @Nonnull
    private Connector[] createConnectors( @Nonnull Server server, @Nonnull MapEx<String, String> cfg )
    {
        List<NetworkConnector> connectors = new LinkedList<>();

        HttpConfiguration httpCfg = createHttpConfiguration( cfg );

        if( cfg.find( "connector.http.enable", Boolean.class ).or( true ) )
        {
            ServerConnector connector = createConnector( server, cfg, "httpConnector", "connector.http.", 8080 );
            connector.addConnectionFactory( createHttpConnectionFactory( httpCfg, cfg, "connector.http." ) );
            connectors.add( connector );
        }

        if( cfg.find( "connector.https.enable", Boolean.class ).or( false ) )
        {
            ServerConnector connector = createConnector( server, cfg, "httpsConnector", "connector.https.", 8443 );
            connector.addConnectionFactory( createHttpsConnectionFactory( cfg, "connector.https." ) );

            HttpConfiguration sslHttpCfg = new HttpConfiguration( httpCfg );
            sslHttpCfg.addCustomizer( new SecureRequestCustomizer() );
            connector.addConnectionFactory( createHttpConnectionFactory( sslHttpCfg, cfg, "connector.https." ) );

            connectors.add( connector );
        }

        return connectors.toArray( new Connector[ connectors.size() ] );
    }

    @Nonnull
    private HttpConfiguration createHttpConfiguration( @Nonnull MapEx<String, String> cfg )
    {
        HttpConfiguration httpCfg = new HttpConfiguration();
        httpCfg.addCustomizer( new ForwardedRequestCustomizer() );
        httpCfg.setHeaderCacheSize( cfg.find( "http.headerCacheSize", Integer.class ).or( 512 ) );
        httpCfg.setOutputBufferSize( cfg.find( "http.outputCacheSize", Integer.class ).or( 32 * 1024 ) );
        httpCfg.setRequestHeaderSize( cfg.find( "http.requestHeaderSize", Integer.class ).or( 8 * 1024 ) );
        httpCfg.setResponseHeaderSize( cfg.find( "http.responseHeaderSize", Integer.class ).or( 8 * 1024 ) );
        httpCfg.setSecurePort( cfg.find( "http.securePort", Integer.class ).or( 8443 ) );
        httpCfg.setSecureScheme( cfg.find( "http.secureScheme" ).or( "https" ) );
        httpCfg.setSendDateHeader( cfg.find( "http.sendDateHeader", Boolean.class ).or( true ) );
        httpCfg.setSendServerVersion( cfg.find( "http.sendServerVersion", Boolean.class ).or( false ) );
        httpCfg.setSendXPoweredBy( cfg.find( "http.sendXPoweredBy", Boolean.class ).or( false ) );
        return httpCfg;
    }

    @Nonnull
    private ServerConnector createConnector( @Nonnull Server server,
                                             @Nonnull MapEx<String, String> cfg,
                                             @Nonnull String name,
                                             @Nonnull String prefix,
                                             int defaultPort )
    {
        ServerConnector httpConnector = new ServerConnector( server );
        httpConnector.setName( name );
        httpConnector.setAcceptQueueSize( cfg.find( prefix + "acceptQueueSize", Integer.class ).or( 1024 ) );
        httpConnector.setHost( cfg.get( prefix + "host" ) );
        httpConnector.setIdleTimeout( cfg.find( prefix + "idleTimeout", Long.class ).or( 30 * 1000l ) );
        httpConnector.setPort( cfg.find( prefix + "port", Integer.class ).or( defaultPort ) );
        httpConnector.setReuseAddress( cfg.find( prefix + "reuseAddress", Boolean.class ).or( true ) );
        httpConnector.setSoLingerTime( cfg.find( prefix + "soLingerTime", Integer.class ).or( -1 ) );
        httpConnector.setStopTimeout( cfg.find( prefix + "stopTimeout", Long.class ).or( 10 * 1000l ) );
        return httpConnector;
    }

    @Nonnull
    private HttpConnectionFactory createHttpConnectionFactory( @Nonnull HttpConfiguration httpCfg,
                                                               @Nonnull MapEx<String, String> cfg,
                                                               @Nonnull String prefix )
    {
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory( httpCfg );
        httpConnectionFactory.setInputBufferSize( cfg.find( prefix + "inputBufferSize", Integer.class ).or( 8192 ) );
        return httpConnectionFactory;
    }

    @Nonnull
    private SslConnectionFactory createHttpsConnectionFactory( @Nonnull MapEx<String, String> cfg,
                                                               @Nonnull String prefix )
    {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setCertAlias( cfg.get( prefix + "certAlias" ) );
        sslContextFactory.setCrlPath( cfg.get( prefix + "crlPath" ) );
        sslContextFactory.setEndpointIdentificationAlgorithm( cfg.get( prefix + "endpointIdentificationAlgorithm" ) );
        sslContextFactory.setExcludeCipherSuites( parseStringArray( cfg, prefix + "excludeCipherSuites" ) );
        sslContextFactory.setExcludeProtocols( parseStringArray( cfg, prefix + "excludeProtocols" ) );
        sslContextFactory.setIncludeCipherSuites( parseStringArray( cfg, prefix + "includeCipherSuites" ) );
        sslContextFactory.setIncludeProtocols( parseStringArray( cfg, prefix + "includeProtocols" ) );
        sslContextFactory.setKeyManagerPassword( cfg.get( prefix + "keyManagerPassword" ) );
        sslContextFactory.setKeyStorePassword( cfg.get( prefix + "keyStorePassword" ) );
        sslContextFactory.setKeyStorePath( cfg.get( prefix + "keyStorePath" ) );
        sslContextFactory.setKeyStoreProvider( cfg.get( prefix + "keyStoreProvider" ) );
        sslContextFactory.setKeyStoreType( cfg.get( prefix + "keyStoreType" ) );
        sslContextFactory.setMaxCertPathLength( cfg.find( prefix + "maxCertPathLength", Integer.class ).or( -1 ) );
        sslContextFactory.setNeedClientAuth( cfg.find( prefix + "needClientAuth", Boolean.class ).or( false ) );
        sslContextFactory.setProtocol( cfg.find( prefix + "protocol" ).or( "TLS" ) );
        sslContextFactory.setProvider( cfg.get( prefix + "provider" ) );
        sslContextFactory.setRenegotiationAllowed( cfg.find( prefix + "renegotiationAllowed", Boolean.class ).or( true ) );
        sslContextFactory.setSecureRandomAlgorithm( cfg.get( prefix + "secureRandomAlgorithm" ) );
        sslContextFactory.setSessionCachingEnabled( cfg.find( prefix + "sessionCachingEnabled", Boolean.class ).or( true ) );
        sslContextFactory.setSslKeyManagerFactoryAlgorithm( cfg.find( prefix + "sslKeyManagerFactoryAlgorithm" ).or( DEFAULT_KEYMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setSslSessionCacheSize( cfg.find( prefix + "sslSessionCacheSize", Integer.class ).or( 0 ) );
        sslContextFactory.setSslSessionTimeout( cfg.find( prefix + "sslSessionTimeout", Integer.class ).or( 0 ) );
        sslContextFactory.setTrustAll( cfg.find( prefix + "trustAll", Boolean.class ).or( false ) );
        sslContextFactory.setTrustManagerFactoryAlgorithm( cfg.find( prefix + "trustManagerFactoryAlgorithm" ).or( DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setTrustStorePassword( cfg.get( prefix + "trustStorePassword" ) );
        sslContextFactory.setTrustStorePath( cfg.get( prefix + "trustStorePath" ) );
        sslContextFactory.setTrustStoreProvider( cfg.get( prefix + "trustStoreProvider" ) );
        sslContextFactory.setTrustStoreType( cfg.find( prefix + "trustStoreType" ).or( "JKS" ) );
        sslContextFactory.setValidateCerts( cfg.find( prefix + "validateCerts", Boolean.class ).or( false ) );
        sslContextFactory.setValidatePeerCerts( cfg.find( prefix + "validatePeerCerts", Boolean.class ).or( false ) );
        sslContextFactory.setWantClientAuth( cfg.find( prefix + "wantClientAuth", Boolean.class ).or( false ) );

        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory( sslContextFactory, "http/1.1" );
        sslConnectionFactory.setInputBufferSize( cfg.find( prefix + "inputBufferSize", Integer.class ).or( 8192 ) );
        return sslConnectionFactory;
    }

    @Nonnull
    private String[] parseStringArray( @Nonnull MapEx<String, String> cfg, @Nonnull String key )
    {
        String value = cfg.get( key );
        if( value != null )
        {
            Iterable<String> values = Splitter.on( "," ).omitEmptyStrings().trimResults().split( value );
            return Iterables.toArray( values, String.class );
        }
        else
        {
            return EMPTY_STRINGS_ARRAY;
        }
    }
}
