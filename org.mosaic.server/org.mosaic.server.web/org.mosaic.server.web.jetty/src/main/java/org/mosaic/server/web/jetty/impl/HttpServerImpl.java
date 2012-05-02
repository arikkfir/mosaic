package org.mosaic.server.web.jetty.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import javax.annotation.PreDestroy;
import org.eclipse.jetty.ajp.Ajp13SocketConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.util.collection.MapAccessor;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_KEYMANAGERFACTORY_ALGORITHM;
import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * @author arik
 */
@Component
public class HttpServerImpl implements HttpServer
{

    private static final Logger LOG = LoggerFactory.getLogger( HttpServerImpl.class );

    private Server jetty;

    private HttpRequestHandler handler;

    @Autowired
    public void setHandler( HttpRequestHandler handler )
    {
        this.handler = handler;
    }

    @ServiceRef( filter = "name=jetty" )
    public void configure( Configuration cfg )
    {
        // if we've already started Jetty - first stop it
        if( this.jetty != null )
        {
            try
            {
                LOG.debug( "Stopping Jetty HTTP server (configuration changed)" );
                this.jetty.stop();
                LOG.info( "Stopped Jetty HTTP server (configuration changed)" );
            }
            catch( Exception e )
            {
                LOG.error( "Could not stop Jetty HTTP server: {}", e.getMessage(), e );
                return;
            }
        }

        // start the server
        LOG.debug( "Starting Jetty HTTP server" );
        try
        {
            Server server = createServer( new MapAccessor<>( cfg ) );
            server.start();
            this.jetty = server;
            LOG.info( "Started Jetty HTTP server" );
        }
        catch( Exception e )
        {
            LOG.error( "Could not start JettyHTTP server: {}", e.getMessage(), e );
        }
    }

    @PreDestroy
    public void destroy()
    {
        if( this.jetty != null )
        {
            try
            {

                LOG.debug( "Stopping Jetty HTTP server (configuration changed)" );
                this.jetty.stop();
                LOG.info( "Stopped Jetty HTTP server (configuration changed)" );

            }
            catch( Exception e )
            {
                LOG.error( "Could not stop Jetty HTTP server: {}", e.getMessage(), e );
            }
        }
    }

    private Server createServer( MapAccessor<String, String> c ) throws IOException
    {
        Server server = new Server();
        server.setConnectors( createConnectors( c ) );
        server.setGracefulShutdown( c.get( "gracefulShutdownTimeout", Integer.class, 5000 ) );
        server.setHandler( this.handler );
        server.setSendDateHeader( c.get( "sendDateHeader", Boolean.class, true ) );
        server.setSendServerVersion( false );
        server.setStopAtShutdown( false );
        server.setThreadPool( createThreadPool( c ) );
        server.setUncheckedPrintWriter( c.get( "uncheckedPrintWriter", Boolean.class, false ) );
        return server;
    }

    private Connector[] createConnectors( MapAccessor<String, String> c ) throws IOException
    {
        Collection<Connector> connectors = new LinkedList<>();
        if( c.get( "http.enabled", Boolean.class, true ) )
        {
            connectors.add( createHttpConnector( c ) );
        }
        if( c.get( "ajp.enabled", Boolean.class, false ) )
        {
            connectors.add( createAjpConnector( c ) );
        }
        if( c.get( "https.enabled", Boolean.class, false ) )
        {
            connectors.add( createHttpsConnector( c ) );
        }
        return connectors.toArray( new Connector[ connectors.size() ] );
    }

    private SelectChannelConnector createHttpConnector( MapAccessor<String, String> c )
    {
        SelectChannelConnector selectChannelConnector = new SelectChannelConnector();
        selectChannelConnector.setAcceptorPriorityOffset( c.get( "http.acceptors.priority.offset", Integer.class, 0 ) );
        selectChannelConnector.setAcceptors( c.get( "http.acceptors.threads", Integer.class, 1 ) );
        selectChannelConnector.setAcceptQueueSize( c.get( "http.acceptors.queue.size", Integer.class, 1024 ) );
        selectChannelConnector.setConfidentialPort( c.get( "http.confidential.port", Integer.class, 443 ) );
        selectChannelConnector.setConfidentialScheme( c.get( "http.confidential.scheme", "https" ) );
        selectChannelConnector.setForwarded( c.get( "http.support.x-forwarded.headers", Boolean.class, true ) );
        selectChannelConnector.setHost( c.get( "http.bind.host" ) );
        selectChannelConnector.setLowResourcesConnections( c.get( "http.low.resources.connections.threshold", Integer.class, 1000 ) );
        selectChannelConnector.setLowResourcesMaxIdleTime( c.get( "http.low.resources.connections.max.idle.millis", Integer.class, 1000 * 5 ) );
        selectChannelConnector.setMaxIdleTime( c.get( "http.max.idle.millis", Integer.class, 1000 * 15 ) );
        selectChannelConnector.setName( "httpConnector" );
        selectChannelConnector.setPort( c.get( "http.port", Integer.class, 8080 ) );
        selectChannelConnector.setRequestBufferSize( c.get( "http.request.buffer.size", Integer.class, 1024 * 4 ) );
        selectChannelConnector.setRequestHeaderSize( c.get( "http.request.header.size", Integer.class, 1024 * 2 ) );
        selectChannelConnector.setResolveNames( c.get( "http.resolve.client.names", Boolean.class, false ) );
        selectChannelConnector.setResponseBufferSize( c.get( "http.response.buffer.size", Integer.class, 1024 * 8 ) );
        selectChannelConnector.setResponseHeaderSize( c.get( "http.response.header.size", Integer.class, 1024 * 2 ) );
        selectChannelConnector.setReuseAddress( c.get( "http.reuse.address", Boolean.class, true ) );
        selectChannelConnector.setSoLingerTime( c.get( "http.socket.linger.time", Integer.class, -1 ) );
        selectChannelConnector.setStatsOn( c.get( "http.statistics", Boolean.class, false ) );
        selectChannelConnector.setUseDirectBuffers( c.get( "http.use.direct.buffers", Boolean.class, true ) );
        return selectChannelConnector;
    }

    private SslConnector createHttpsConnector( MapAccessor<String, String> c ) throws IOException
    {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setAllowRenegotiate( true );
        sslContextFactory.setCertAlias( c.get( "https.certificate.alias" ) );
        sslContextFactory.setCrlPath( c.get( "https.crl.path" ) );
        sslContextFactory.setExcludeCipherSuites( tokenizeToStringArray( c.get( "https.ciphers.excludes", "" ), ",:\n\r\f\t " ) );
        sslContextFactory.setIncludeCipherSuites( tokenizeToStringArray( c.get( "https.ciphers.includes", "" ), ",:\n\r\f\t " ) );
        sslContextFactory.setKeyManagerPassword( fs( c, "https.key.manager.password" ) );
        sslContextFactory.setKeyStorePath( c.get( "https.key.store.path" ) );
        sslContextFactory.setKeyStorePassword( fs( c, "https.key.store.password" ) );
        sslContextFactory.setKeyStoreProvider( c.get( "https.key.store.provider" ) );
        sslContextFactory.setKeyStoreType( c.get( "https.key.store.type", "JKS" ) );
        sslContextFactory.setMaxCertPathLength( c.get( "https.certificate.max.length", Integer.class, 99 ) );
        sslContextFactory.setNeedClientAuth( c.get( "https.need.client.auth", Boolean.class, false ) );
        sslContextFactory.setProtocol( c.get( "https.protocol", "TLS" ) );
        sslContextFactory.setProvider( c.get( "https.provider" ) );
        sslContextFactory.setSecureRandomAlgorithm( c.get( "https.secure.random.algorithm" ) );
        sslContextFactory.setSslKeyManagerFactoryAlgorithm( c.get( "https.ssl.key.manager.factory.algorithm", DEFAULT_KEYMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setTrustManagerFactoryAlgorithm( c.get( "https.trust.manager.factory.algorithm", DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM ) );

        String trustStorePath = c.get( "https.trust.store.path" );
        if( hasText( trustStorePath ) )
        {
            sslContextFactory.setTrustStore( trustStorePath );
            sslContextFactory.setTrustStorePassword( fs( c, "https.trust.store.password" ) );
            sslContextFactory.setTrustStoreProvider( c.get( "https.trust.store.provider" ) );
            sslContextFactory.setTrustStoreType( c.get( "https.trust.store.type", "JKS" ) );
        }
        sslContextFactory.setValidateCerts( c.get( "https.validate.certificates", Boolean.class, true ) );
        sslContextFactory.setWantClientAuth( c.get( "https.want.client.auth", Boolean.class, false ) );

        SslSelectChannelConnector connector = new SslSelectChannelConnector( sslContextFactory );
        connector.setAcceptorPriorityOffset( c.get( "https.acceptors.priority.offset", Integer.class, 0 ) );
        connector.setAcceptors( c.get( "https.acceptors.threads", Integer.class, 1 ) );
        connector.setAcceptQueueSize( c.get( "https.acceptors.queue.size", Integer.class, 1024 ) );
        connector.setConfidentialPort( c.get( "https.confidential.port", Integer.class, 8443 ) );
        connector.setConfidentialScheme( c.get( "https.confidential.scheme", "https" ) );
        connector.setForwarded( c.get( "https.support.x-forwarded.headers", Boolean.class, true ) );
        connector.setHost( c.get( "https.bind.host" ) );
        connector.setLowResourcesConnections( c.get( "https.low.resources.connections.threshold", Integer.class, 1000 ) );
        connector.setLowResourcesMaxIdleTime( c.get( "https.low.resources.connections.max.idle.millis", Integer.class, 1000 * 5 ) );
        connector.setMaxIdleTime( c.get( "https.max.idle.millis", Integer.class, 1000 * 15 ) );
        connector.setName( "httpsConnector" );
        connector.setPort( c.get( "https.port", Integer.class, 8443 ) );
        connector.setRequestBufferSize( c.get( "https.request.buffer.size", Integer.class, 1024 * 4 ) );
        connector.setRequestHeaderSize( c.get( "https.request.header.size", Integer.class, 1024 * 2 ) );
        connector.setResolveNames( c.get( "https.resolve.client.names", Boolean.class, false ) );
        connector.setResponseBufferSize( c.get( "https.response.buffer.size", Integer.class, 1024 * 8 ) );
        connector.setResponseHeaderSize( c.get( "https.response.header.size", Integer.class, 1024 * 2 ) );
        connector.setReuseAddress( c.get( "https.reuse.address", Boolean.class, true ) );
        connector.setSoLingerTime( c.get( "https.socket.linger.time", Integer.class, -1 ) );
        connector.setStatsOn( c.get( "https.statistics", Boolean.class, false ) );
        connector.setUseDirectBuffers( c.get( "https.use.direct.buffers", Boolean.class, true ) );
        return connector;
    }

    private Ajp13SocketConnector createAjpConnector( MapAccessor<String, String> c )
    {
        Ajp13SocketConnector connector = new Ajp13SocketConnector();
        connector.setAcceptorPriorityOffset( c.get( "ajp.acceptors.priority.offset", Integer.class, 0 ) );
        connector.setAcceptors( c.get( "ajp.acceptors.threads", Integer.class, 1 ) );
        connector.setAcceptQueueSize( c.get( "ajp.acceptors.queue.size", Integer.class, 1024 ) );
        connector.setConfidentialPort( c.get( "ajp.confidential.port", Integer.class, 8443 ) );
        connector.setConfidentialScheme( c.get( "ajp.confidential.scheme", "https" ) );
        connector.setForwarded( c.get( "ajp.support.x-forwarded.headers", Boolean.class, true ) );
        connector.setHost( c.get( "ajp.bind.host" ) );
        connector.setLowResourcesMaxIdleTime( c.get( "ajp.low.resources.connections.max.idle.millis", Integer.class, 1000 * 5 ) );
        connector.setMaxIdleTime( c.get( "ajp.max.idle.millis", Integer.class, 1000 * 15 ) );
        connector.setName( "ajpConnector" );
        connector.setPort( c.get( "ajp.port", Integer.class, 8080 ) );
        connector.setRequestBufferSize( c.get( "ajp.request.buffer.size", Integer.class, 1024 * 12 ) );
        connector.setRequestHeaderSize( c.get( "ajp.request.header.size", Integer.class, 1024 * 6 ) );
        connector.setResolveNames( c.get( "ajp.resolve.client.names", Boolean.class, false ) );
        connector.setResponseBufferSize( c.get( "ajp.response.buffer.size", Integer.class, 1024 * 12 ) );
        connector.setResponseHeaderSize( c.get( "ajp.response.header.size", Integer.class, 1024 * 6 ) );
        connector.setReuseAddress( c.get( "ajp.reuse.address", Boolean.class, true ) );
        connector.setSoLingerTime( c.get( "ajp.socket.linger.time", Integer.class, -1 ) );
        connector.setStatsOn( c.get( "ajp.statistics", Boolean.class, false ) );
        return connector;
    }

    private ThreadPool createThreadPool( MapAccessor<String, String> c )
    {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon( true );
        threadPool.setMaxIdleTimeMs( c.get( "jetty.thread.pool.max.idle.time.millis", Integer.class, 1000 * 60 ) );
        threadPool.setMaxQueued( c.get( "jetty.thread.pool.max.queued", Integer.class, 1000 ) );
        threadPool.setMaxStopTimeMs( c.get( "jetty.thread.pool.max.stop.time.millis", Integer.class, 0 ) );
        threadPool.setMaxThreads( c.get( "jetty.thread.pool.max.threads", Integer.class, 500 ) );
        threadPool.setMinThreads( c.get( "jetty.thread.pool.min.threads", Integer.class, 1 ) );
        threadPool.setName( "org.mosaic.server.jetty" );
        return threadPool;
    }

    private String fs( MapAccessor<String, String> c, String key ) throws IOException
    {
        return fs( c, key, null );
    }

    private String fs( MapAccessor<String, String> c, String key, String defaultValue ) throws IOException
    {
        String value = c.get( key );
        if( !hasText( value ) )
        {
            return defaultValue;
        }

        Path path = Paths.get( value );
        if( !Files.exists( path ) )
        {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') could not be found", path, key );
            return defaultValue;

        }
        else if( Files.isDirectory( path ) )
        {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') points to a directory and not a file", path, key );
            return defaultValue;

        }
        else if( !Files.isReadable( path ) )
        {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') could not be read", path, key );
            return defaultValue;
        }

        String contents = new String( Files.readAllBytes( path ), "UTF-8" ).trim();
        return contents.length() == 0 ? defaultValue : contents.trim();
    }
}
