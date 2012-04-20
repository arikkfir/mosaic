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
import org.joda.time.Duration;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.web.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_KEYMANAGERFACTORY_ALGORITHM;
import static org.eclipse.jetty.util.ssl.SslContextFactory.DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM;
import static org.joda.time.Duration.standardSeconds;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * @author arik
 */
@Component
public class HttpServerImpl implements HttpServer {

    private static final Logger LOG = LoggerFactory.getLogger( HttpServerImpl.class );

    private Server jetty;

    private Configuration cfg;

    private HttpApplicationJettyHandler handler;

    @Autowired
    public void setHandler( HttpApplicationJettyHandler handler ) {
        this.handler = handler;
    }

    @ServiceRef( filter = "name=jetty" )
    public void configure( Configuration cfg ) {

        // if we've already started Jetty - first stop it
        if( this.jetty != null ) {
            try {

                LOG.debug( "Stopping Jetty HTTP server (configuration changed)" );
                this.jetty.stop();
                LOG.info( "Stopped Jetty HTTP server (configuration changed)" );

            } catch( Exception e ) {
                LOG.error( "Could not stop Jetty HTTP server: {}", e.getMessage(), e );
                return;
            }
        }

        // start the server
        LOG.debug( "Starting Jetty HTTP server" );
        try {

            this.cfg = cfg;
            Server server = createServer();
            server.start();
            this.jetty = server;
            LOG.info( "Started Jetty HTTP server" );

        } catch( Exception e ) {
            LOG.error( "Could not start JettyHTTP server: {}", e.getMessage(), e );
        }
    }

    @PreDestroy
    public void destroy() {
        if( this.jetty != null ) {
            try {

                LOG.debug( "Stopping Jetty HTTP server (configuration changed)" );
                this.jetty.stop();
                LOG.info( "Stopped Jetty HTTP server (configuration changed)" );

            } catch( Exception e ) {
                LOG.error( "Could not stop Jetty HTTP server: {}", e.getMessage(), e );
            }
        }
    }

    private Server createServer() throws IOException {
        Server server = new Server();
        server.setConnectors( createConnectors() );
        server.setGracefulShutdown( duration( "gracefulShutdownTimeout", standardSeconds( 30 ) ) );
        server.setHandler( this.handler );
        server.setSendDateHeader( b( "sendDateHeader", true ) );
        server.setSendServerVersion( false );
        server.setStopAtShutdown( false );
        server.setThreadPool( createThreadPool() );
        server.setUncheckedPrintWriter( b( "uncheckedPrintWriter", false ) );
        return server;
    }

    private Connector[] createConnectors() throws IOException {
        Collection<Connector> connectors = new LinkedList<>();
        if( b( "http.enabled", true ) ) {
            connectors.add( createHttpConnector() );
        }
        if( b( "ajp.enabled", false ) ) {
            connectors.add( createAjpConnector() );
        }
        if( b( "https.enabled", false ) ) {
            connectors.add( createHttpsConnector() );
        }
        return connectors.toArray( new Connector[ connectors.size() ] );
    }

    private SelectChannelConnector createHttpConnector() {
        SelectChannelConnector selectChannelConnector = new SelectChannelConnector();
        selectChannelConnector.setAcceptorPriorityOffset( i( "http.acceptors.priority.offset", 0 ) );
        selectChannelConnector.setAcceptors( i( "http.acceptors.threads", 1 ) );
        selectChannelConnector.setAcceptQueueSize( i( "http.acceptors.queue.size", 1024 ) );
        selectChannelConnector.setConfidentialPort( i( "http.confidential.port", 443 ) );
        selectChannelConnector.setConfidentialScheme( s( "http.confidential.scheme", "https" ) );
        selectChannelConnector.setForwarded( b( "http.support.x-forwarded.headers", true ) );
        selectChannelConnector.setHost( s( "http.bind.host" ) );
        selectChannelConnector.setLowResourcesConnections( i( "http.low.resources.connections.threshold", 1000 ) );
        selectChannelConnector.setLowResourcesMaxIdleTime( i( "http.low.resources.connections.max.idle.millis", 1000 * 5 ) );
        selectChannelConnector.setMaxIdleTime( i( "http.max.idle.millis", 1000 * 15 ) );
        selectChannelConnector.setName( "httpConnector" );
        selectChannelConnector.setPort( i( "http.port", 8080 ) );
        selectChannelConnector.setRequestBufferSize( i( "http.request.buffer.size", 1024 * 4 ) );
        selectChannelConnector.setRequestHeaderSize( i( "http.request.header.size", 1024 * 2 ) );
        selectChannelConnector.setResolveNames( b( "http.resolve.client.names", false ) );
        selectChannelConnector.setResponseBufferSize( i( "http.response.buffer.size", 1024 * 8 ) );
        selectChannelConnector.setResponseHeaderSize( i( "http.response.header.size", 1024 * 2 ) );
        selectChannelConnector.setReuseAddress( b( "http.reuse.address", true ) );
        selectChannelConnector.setSoLingerTime( i( "http.socket.linger.time", -1 ) );
        selectChannelConnector.setStatsOn( b( "http.statistics", false ) );
        selectChannelConnector.setUseDirectBuffers( b( "http.use.direct.buffers", true ) );
        return selectChannelConnector;
    }

    private SslConnector createHttpsConnector() throws IOException {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setAllowRenegotiate( true );
        sslContextFactory.setCertAlias( s( "https.certificate.alias" ) );
        sslContextFactory.setCrlPath( s( "https.crl.path" ) );
        sslContextFactory.setExcludeCipherSuites( tokenizeToStringArray( s( "https.ciphers.excludes", "" ), ",:\n\r\f\t " ) );
        sslContextFactory.setIncludeCipherSuites( tokenizeToStringArray( s( "https.ciphers.includes", "" ), ",:\n\r\f\t " ) );
        sslContextFactory.setKeyManagerPassword( fs( "https.key.manager.password" ) );
        sslContextFactory.setKeyStorePath( s( "https.key.store.path" ) );
        sslContextFactory.setKeyStorePassword( fs( "https.key.store.password" ) );
        sslContextFactory.setKeyStoreProvider( s( "https.key.store.provider" ) );
        sslContextFactory.setKeyStoreType( s( "https.key.store.type", "JKS" ) );
        sslContextFactory.setMaxCertPathLength( i( "https.certificate.max.length", 99 ) );
        sslContextFactory.setNeedClientAuth( b( "https.need.client.auth", false ) );
        sslContextFactory.setProtocol( s( "https.protocol", "TLS" ) );
        sslContextFactory.setProvider( s( "https.provider" ) );
        sslContextFactory.setSecureRandomAlgorithm( s( "https.secure.random.algorithm" ) );
        sslContextFactory.setSslKeyManagerFactoryAlgorithm( s( "https.ssl.key.manager.factory.algorithm", DEFAULT_KEYMANAGERFACTORY_ALGORITHM ) );
        sslContextFactory.setTrustManagerFactoryAlgorithm( s( "https.trust.manager.factory.algorithm", DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM ) );

        String trustStorePath = s( "https.trust.store.path" );
        if( hasText( trustStorePath ) ) {
            sslContextFactory.setTrustStore( trustStorePath );
            sslContextFactory.setTrustStorePassword( fs( "https.trust.store.password" ) );
            sslContextFactory.setTrustStoreProvider( s( "https.trust.store.provider" ) );
            sslContextFactory.setTrustStoreType( s( "https.trust.store.type", "JKS" ) );
        }
        sslContextFactory.setValidateCerts( b( "https.validate.certificates", true ) );
        sslContextFactory.setWantClientAuth( b( "https.want.client.auth", false ) );

        SslSelectChannelConnector connector = new SslSelectChannelConnector( sslContextFactory );
        connector.setAcceptorPriorityOffset( i( "https.acceptors.priority.offset", 0 ) );
        connector.setAcceptors( i( "https.acceptors.threads", 1 ) );
        connector.setAcceptQueueSize( i( "https.acceptors.queue.size", 1024 ) );
        connector.setConfidentialPort( i( "https.confidential.port", 8443 ) );
        connector.setConfidentialScheme( s( "https.confidential.scheme", "https" ) );
        connector.setForwarded( b( "https.support.x-forwarded.headers", true ) );
        connector.setHost( s( "https.bind.host" ) );
        connector.setLowResourcesConnections( i( "https.low.resources.connections.threshold", 1000 ) );
        connector.setLowResourcesMaxIdleTime( i( "https.low.resources.connections.max.idle.millis", 1000 * 5 ) );
        connector.setMaxIdleTime( i( "https.max.idle.millis", 1000 * 15 ) );
        connector.setName( "httpsConnector" );
        connector.setPort( i( "https.port", 8443 ) );
        connector.setRequestBufferSize( i( "https.request.buffer.size", 1024 * 4 ) );
        connector.setRequestHeaderSize( i( "https.request.header.size", 1024 * 2 ) );
        connector.setResolveNames( b( "https.resolve.client.names", false ) );
        connector.setResponseBufferSize( i( "https.response.buffer.size", 1024 * 8 ) );
        connector.setResponseHeaderSize( i( "https.response.header.size", 1024 * 2 ) );
        connector.setReuseAddress( b( "https.reuse.address", true ) );
        connector.setSoLingerTime( i( "https.socket.linger.time", -1 ) );
        connector.setStatsOn( b( "https.statistics", false ) );
        connector.setUseDirectBuffers( b( "https.use.direct.buffers", true ) );
        return connector;
    }

    private Ajp13SocketConnector createAjpConnector() {
        Ajp13SocketConnector connector = new Ajp13SocketConnector();
        connector.setAcceptorPriorityOffset( i( "ajp.acceptors.priority.offset", 0 ) );
        connector.setAcceptors( i( "ajp.acceptors.threads", 1 ) );
        connector.setAcceptQueueSize( i( "ajp.acceptors.queue.size", 1024 ) );
        connector.setConfidentialPort( i( "ajp.confidential.port", 8443 ) );
        connector.setConfidentialScheme( s( "ajp.confidential.scheme", "https" ) );
        connector.setForwarded( b( "ajp.support.x-forwarded.headers", true ) );
        connector.setHost( s( "ajp.bind.host" ) );
        connector.setLowResourcesMaxIdleTime( i( "ajp.low.resources.connections.max.idle.millis", 1000 * 5 ) );
        connector.setMaxIdleTime( i( "ajp.max.idle.millis", 1000 * 15 ) );
        connector.setName( "ajpConnector" );
        connector.setPort( i( "ajp.port", 8080 ) );
        connector.setRequestBufferSize( i( "ajp.request.buffer.size", 1024 * 12 ) );
        connector.setRequestHeaderSize( i( "ajp.request.header.size", 1024 * 6 ) );
        connector.setResolveNames( b( "ajp.resolve.client.names", false ) );
        connector.setResponseBufferSize( i( "ajp.response.buffer.size", 1024 * 12 ) );
        connector.setResponseHeaderSize( i( "ajp.response.header.size", 1024 * 6 ) );
        connector.setReuseAddress( b( "ajp.reuse.address", true ) );
        connector.setSoLingerTime( i( "ajp.socket.linger.time", -1 ) );
        connector.setStatsOn( b( "ajp.statistics", false ) );
        return connector;
    }

    private ThreadPool createThreadPool() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon( true );
        threadPool.setMaxIdleTimeMs( i( "jetty.thread.pool.max.idle.time.millis", 1000 * 60 ) );
        threadPool.setMaxQueued( i( "jetty.thread.pool.max.queued", 1000 ) );
        threadPool.setMaxStopTimeMs( i( "jetty.thread.pool.max.stop.time.millis", 0 ) );
        threadPool.setMaxThreads( i( "jetty.thread.pool.max.threads", 500 ) );
        threadPool.setMinThreads( i( "jetty.thread.pool.min.threads", 1 ) );
        threadPool.setName( "com.infolinks.rinku.server.jetty" );
        return threadPool;
    }

    private Integer i( String key, int defaultValue ) {
        return this.cfg.getValueAs( key, Integer.class, defaultValue );
    }

    private Boolean b( String key, boolean defaultValue ) {
        return this.cfg.getValueAs( key, Boolean.class, defaultValue );
    }

    private String s( String key ) {
        return this.cfg.getValue( key );
    }

    private String s( String key, String defaultValue ) {
        return this.cfg.getValue( key, defaultValue );
    }

    private String fs( String key ) throws IOException {
        return fs( key, null );
    }

    private String fs( String key, String defaultValue ) throws IOException {
        String value = s( key );
        if( !hasText( value ) ) {
            return defaultValue;
        }

        Path path = Paths.get( value );
        if( !Files.exists( path ) ) {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') could not be found", path, key );
            return defaultValue;

        } else if( Files.isDirectory( path ) ) {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') points to a directory and not a file", path, key );
            return defaultValue;

        } else if( !Files.isReadable( path ) ) {
            LOG.warn( "File '{}' (referenced by Jetty configuration key '{}') could not be read", path, key );
            return defaultValue;
        }

        String contents = new String( Files.readAllBytes( path ), "UTF-8" ).trim();
        return contents.length() == 0 ? defaultValue : contents.trim();
    }

    private int duration( String key, Duration defaultValue ) {
        return ( int ) cfg.getValueAs( key, Duration.class, defaultValue ).getMillis();
    }

}
