package org.mosaic.web.impl;

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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
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

    @Nullable
    private Server server;

    @Component
    @Nonnull
    private Module module;

    @Nonnull
    @Component
    private RequestDispatcher requestDispatcher;

    @Nullable
    private ContextHandlerCollection contextHandlerCollection;

    @Configurable( "web" )
    void configure( @Nonnull final MapEx<String, String> cfg )
    {
        LOG.info( "Web server configured - {}", this.server != null ? "restarting" : "starting" );
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
        final Server server = this.server;
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
        Application application = reference.require();
        this.applications.add( application );
        addContext( application );
    }

    @OnServiceRemoved
    void onApplicationRemoved( @Nonnull ServiceReference<Application> reference )
    {
        Application application = reference.require();
        removeContext( application );
        this.applications.remove( application );
    }

    private void addContext( @Nonnull Application application )
    {
        removeContext( application );

        ContextHandlerCollection contextHandlerCollection = this.contextHandlerCollection;
        if( contextHandlerCollection != null )
        {
            ServletContextHandler contextHandler = new ServletContextHandler( ServletContextHandler.SESSIONS );
            contextHandler.setAllowNullPathInfo( false );
            contextHandler.setAttribute( Application.class.getName(), application );
            contextHandler.setCompactPath( true );
            contextHandler.setDisplayName( application.getName() );
            contextHandler.setErrorHandler( new NoErrorPageErrorHandler() );
            contextHandler.setVirtualHosts( Iterables.toArray( application.getVirtualHosts(), String.class ) );
            contextHandler.addFilter( MultiPartFilter.class, "/", EnumSet.of( DispatcherType.REQUEST ) );
            contextHandler.addFilter( GzipFilter.class, "/", EnumSet.of( DispatcherType.REQUEST ) ); // TODO: configure gzip filter
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
        Server server = this.server;

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
            server.setHandler( new ContextHandlerCollection() );
            server.start();
            this.server = server;

            this.contextHandlerCollection = ( ContextHandlerCollection ) this.server.getHandler();

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
            this.server = null;
        }
    }

    private Server createServer( MapEx<String, String> cfg )
    {
        Server server = new Server();
        server.setDumpAfterStart( false );
        server.setDumpBeforeStop( false );
        server.setStopAtShutdown( false ); // mosaic will close the module on jvm close anyway
        server.setStopTimeout( cfg.get( "stopTimeout", Long.class, 60 * 1000l ) );
        server.setConnectors( createConnectors( server, cfg ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormContentSize", cfg.get( "maxFormContentSize", Integer.class, 100000 ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormKeys", cfg.get( "maxFormKeys", Integer.class, 2000 ) );
        return server;
    }

    @Nonnull
    private Connector[] createConnectors( @Nonnull Server server, @Nonnull MapEx<String, String> cfg )
    {
        List<NetworkConnector> connectors = new LinkedList<>();

        HttpConfiguration httpCfg = createHttpConfiguration( cfg );

        if( cfg.get( "connector.http.enable", Boolean.class, true ) )
        {
            ServerConnector connector = createConnector( server, cfg, "httpConnector", "connector.http.", 8080 );
            connector.addConnectionFactory( createHttpConnectionFactory( httpCfg, cfg, "connector.http." ) );
            connectors.add( connector );
        }

        if( cfg.get( "connector.https.enable", Boolean.class, false ) )
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
        httpCfg.setHeaderCacheSize( cfg.get( "http.headerCacheSize", Integer.class, 512 ) );
        httpCfg.setOutputBufferSize( cfg.get( "http.outputCacheSize", Integer.class, 32 * 1024 ) );
        httpCfg.setRequestHeaderSize( cfg.get( "http.requestHeaderSize", Integer.class, 8 * 1024 ) );
        httpCfg.setResponseHeaderSize( cfg.get( "http.responseHeaderSize", Integer.class, 8 * 1024 ) );
        httpCfg.setSecurePort( cfg.get( "http.securePort", Integer.class, 8443 ) );
        httpCfg.setSecureScheme( cfg.get( "http.secureScheme", String.class, "https" ) );
        httpCfg.setSendDateHeader( cfg.get( "http.sendDateHeader", Boolean.class, true ) );
        httpCfg.setSendServerVersion( cfg.get( "http.sendServerVersion", Boolean.class, false ) );
        httpCfg.setSendXPoweredBy( cfg.get( "http.sendXPoweredBy", Boolean.class, false ) );
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
        httpConnector.setAcceptQueueSize( cfg.get( prefix + "acceptQueueSize", Integer.class, 1024 ) );
        httpConnector.setHost( cfg.get( prefix + "host", String.class ) );
        httpConnector.setIdleTimeout( cfg.get( prefix + "idleTimeout", Long.class, 30 * 1000l ) );
        httpConnector.setPort( cfg.get( prefix + "port", Integer.class, defaultPort ) );
        httpConnector.setReuseAddress( cfg.get( prefix + "reuseAddress", Boolean.class, true ) );
        httpConnector.setSoLingerTime( cfg.get( prefix + "soLingerTime", Integer.class, -1 ) );
        httpConnector.setStopTimeout( cfg.get( prefix + "stopTimeout", Long.class, 10 * 1000l ) );
        return httpConnector;
    }

    @Nonnull
    private HttpConnectionFactory createHttpConnectionFactory( @Nonnull HttpConfiguration httpCfg,
                                                               @Nonnull MapEx<String, String> cfg,
                                                               @Nonnull String prefix )
    {
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory( httpCfg );
        httpConnectionFactory.setInputBufferSize( cfg.get( prefix + "inputBufferSize", Integer.class, 8192 ) );
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
        sslContextFactory.setMaxCertPathLength( cfg.get( prefix + "maxCertPathLength", Integer.class, -1 ) );
        sslContextFactory.setNeedClientAuth( cfg.get( prefix + "needClientAuth", Boolean.class, false ) );
        sslContextFactory.setProtocol( cfg.get( prefix + "protocol", String.class, "TLS" ) );
        sslContextFactory.setProvider( cfg.get( prefix + "provider", String.class ) );
        sslContextFactory.setRenegotiationAllowed( cfg.get( prefix + "renegotiationAllowed", Boolean.class, true ) );
        sslContextFactory.setSecureRandomAlgorithm( cfg.get( prefix + "secureRandomAlgorithm", String.class ) );
        sslContextFactory.setSessionCachingEnabled( cfg.get( prefix + "sessionCachingEnabled", Boolean.class, true ) );
        sslContextFactory.setSslKeyManagerFactoryAlgorithm( cfg.get( prefix + "sslKeyManagerFactoryAlgorithm", String.class, DEFAULT_KEYMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setSslSessionCacheSize( cfg.get( prefix + "sslSessionCacheSize", Integer.class, 0 ) );
        sslContextFactory.setSslSessionTimeout( cfg.get( prefix + "sslSessionTimeout", Integer.class, 0 ) );
        sslContextFactory.setTrustAll( cfg.get( prefix + "trustAll", Boolean.class, false ) );
        sslContextFactory.setTrustManagerFactoryAlgorithm( cfg.get( prefix + "trustManagerFactoryAlgorithm", String.class, DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setTrustStorePassword( cfg.get( prefix + "trustStorePassword", String.class ) );
        sslContextFactory.setTrustStorePath( cfg.get( prefix + "trustStorePath", String.class ) );
        sslContextFactory.setTrustStoreProvider( cfg.get( prefix + "trustStoreProvider", String.class ) );
        sslContextFactory.setTrustStoreType( cfg.get( prefix + "trustStoreType", String.class, "JKS" ) );
        sslContextFactory.setValidateCerts( cfg.get( prefix + "validateCerts", Boolean.class, false ) );
        sslContextFactory.setValidatePeerCerts( cfg.get( prefix + "validatePeerCerts", Boolean.class, false ) );
        sslContextFactory.setWantClientAuth( cfg.get( prefix + "wantClientAuth", Boolean.class, false ) );

        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory( sslContextFactory, "http/1.1" );
        sslConnectionFactory.setInputBufferSize( cfg.get( prefix + "inputBufferSize", Integer.class, 8192 ) );
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
