package org.mosaic.web.server.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Configurable;
import org.mosaic.util.collect.EmptyMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.impl.RequestHandler;
import org.mosaic.web.server.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class WebServerImpl implements WebServer
{
    private static final Logger LOG = LoggerFactory.getLogger( WebServerImpl.class );

    @Nonnull
    private RequestHandler requestHandler;

    @Nullable
    private Server server;

    @Nonnull
    private MapEx<String, String> cfg = EmptyMapEx.emptyMapEx();

    @BeanRef
    public void setRequestHandler( @Nonnull RequestHandler requestHandler )
    {
        this.requestHandler = requestHandler;
    }

    @Override
    public synchronized void start() throws Exception
    {
        if( this.server == null )
        {
            stop();
            try
            {
                Server server = createServer();
                server.start();
                this.server = server;

                LOG.info( "Started web server listening at the following addresses:" );
                for( Connector connector : this.server.getConnectors() )
                {
                    if( connector instanceof NetworkConnector )
                    {
                        NetworkConnector networkConnector = ( NetworkConnector ) connector;
                        String host = networkConnector.getHost();
                        int port = networkConnector.getPort();
                        LOG.info( "    {}:{}", host == null ? "(all)" : host, port );
                    }
                }
            }
            catch( Exception e )
            {
                LOG.warn( "Could not start web server: {}", e.getMessage(), e );
            }
        }
    }

    @Override
    @PreDestroy
    public synchronized void stop() throws Exception
    {
        if( this.server != null )
        {
            try
            {
                this.server.stop();
                this.server.join();
                LOG.info( "Stopped web server" );
            }
            catch( Exception e )
            {
                LOG.warn( "Could not stop web server: {}", e.getMessage(), e );
            }
            finally
            {
                this.server = null;
            }
        }
    }

    @Configurable( "web" )
    public void configure( @Nonnull MapEx<String, String> cfg ) throws Exception
    {
        Log.setLog( new Slf4jLog() );
        this.cfg = cfg;

        if( cfg.get( "enable", Boolean.class, false ) )
        {
            start();
        }
        else
        {
            LOG.info( "Web server disabled (add 'enable=true' to your web.properties file to enable)" );
            stop();
        }
    }

    private Server createServer()
    {
        Server server = new Server();
        server.setStopAtShutdown( true );
        server.setStopTimeout( this.cfg.get( "stopTimeout", Long.class, 60 * 1000l ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormContentSize", this.cfg.get( "server.maxFormContentSize", Integer.class, 1024 * 200 ) );
        server.setAttribute( "org.eclipse.jetty.server.Request.maxFormKeys", this.cfg.get( "server.maxFormKeys", Integer.class, 2000 ) );
        server.addConnector( createConnector( this.cfg, server ) );
        server.setHandler( this.requestHandler );
        return server;
    }

    private ServerConnector createConnector( MapEx<String, String> cfg, Server server )
    {
        ServerConnector connector = new ServerConnector( server, createHttpConnectionFactory( cfg ) );
        connector.setReuseAddress( cfg.get( "connector.reuseAddress", Boolean.class, false ) );
        connector.setDefaultProtocol( "http/1.1" );
        connector.setPort( cfg.get( "connector.port", Integer.class, 8080 ) );
        connector.setIdleTimeout( cfg.get( "connector.idleTimeout", Integer.class, 60000 ) );
        connector.setHost( cfg.get( "connector.host", String.class, "localhost" ) );
        return connector;
    }

    private HttpConnectionFactory createHttpConnectionFactory( MapEx<String, String> cfg )
    {
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory( createHttpConfiguration( cfg ) );
        httpConnectionFactory.setInputBufferSize( cfg.get( "http.inputBufferSize", Integer.class, 1024 * 8 ) );
        return httpConnectionFactory;
    }

    private HttpConfiguration createHttpConfiguration( MapEx<String, String> cfg )
    {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setHeaderCacheSize( cfg.get( "http.headerCacheSize", Integer.class, 1024 * 8 ) );
        httpConfiguration.setOutputBufferSize( cfg.get( "http.outputBufferSize", Integer.class, 1024 * 32 ) );
        httpConfiguration.setRequestHeaderSize( cfg.get( "http.requestHeaderSize", Integer.class, 1024 * 8 ) );
        httpConfiguration.setResponseHeaderSize( cfg.get( "http.responseHeaderSize", Integer.class, 1024 * 8 ) );
        httpConfiguration.setSecurePort( cfg.get( "http.securePort", Integer.class, 8443 ) );
        httpConfiguration.setSecureScheme( cfg.get( "http.secureScheme", String.class, "https" ) );
        httpConfiguration.setSendDateHeader( cfg.get( "http.sendDateHeader", Boolean.class, true ) );
        httpConfiguration.setSendServerVersion( cfg.get( "http.sendServerVersion", Boolean.class, false ) );
        httpConfiguration.addCustomizer( new ForwardedRequestCustomizer() );
        return httpConfiguration;
    }
}
