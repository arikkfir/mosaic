package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.SessionFactory;
import org.mosaic.MosaicHome;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.server.shell.impl.session.MosaicServerSession;
import org.mosaic.server.shell.impl.session.Shell;
import org.mosaic.server.shell.impl.session.UserAuthLocalhostNone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class MosaicSshServer {

    private static final Logger LOG = LoggerFactory.getBundleLogger( MosaicSshServer.class );

    private MosaicHome home;

    private Configuration configuration;

    private SshServer sshServer;

    private PasswordAuthenticator passwordAuthenticator;

    private PublickeyAuthenticator publicKeyAuthenticator;

    private ApplicationContext applicationContext;

    @Autowired
    public void setPasswordAuthenticator( PasswordAuthenticator passwordAuthenticator ) {
        this.passwordAuthenticator = passwordAuthenticator;
    }

    @Autowired
    public void setPublicKeyAuthenticator( PublickeyAuthenticator publicKeyAuthenticator ) {
        this.publicKeyAuthenticator = publicKeyAuthenticator;
    }

    @Autowired
    public void setApplicationContext( ApplicationContext applicationContext ) {
        this.applicationContext = applicationContext;
    }

    @ServiceRef( filter = "name=ssh" )
    public void setConfiguration( Configuration configuration ) throws InterruptedException {
        this.configuration = configuration;
        if( this.sshServer != null ) {
            stop();
            init();
        }
    }

    @ServiceRef
    public void setHome( MosaicHome home ) {
        this.home = home;
    }

    @PostConstruct
    public synchronized void init() throws InterruptedException {
        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort( this.configuration.require( "port", Integer.class, 9080 ) );
        this.sshServer.setShellFactory( new MosaicSshShellFactory() );
        this.sshServer.setPasswordAuthenticator( this.passwordAuthenticator );
        this.sshServer.setUserAuthFactories( createUserAuthFactories() );
        this.sshServer.setKeyPairProvider( new SimpleGeneratorHostKeyProvider( this.home.getWork().resolve( "host.ser" ).toString() ) );
        this.sshServer.setPublickeyAuthenticator( this.publicKeyAuthenticator );
        this.sshServer.setSessionFactory( new SessionFactory() {
            @Override
            protected AbstractSession doCreateSession( IoSession ioSession ) throws Exception {
                return new MosaicServerSession( server, ioSession );
            }
        } );

        try {
            this.sshServer.start();
        } catch( IOException e ) {
            LOG.warn( "Could not start Mosaic shell service: {}", e.getMessage(), e );
        }
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if( this.sshServer != null ) {
            this.sshServer.stop( false );
        }
    }

    private List<NamedFactory<UserAuth>> createUserAuthFactories() {
        List<NamedFactory<UserAuth>> factories = new ArrayList<>();
        factories.add( new UserAuthLocalhostNone.Factory() );
        factories.add( new UserAuthPassword.Factory() );
        factories.add( new UserAuthPublicKey.Factory() );
        return factories;
    }

    private class MosaicSshShellFactory implements Factory<Command> {

        @Override
        public Command create() {
            Shell shell = applicationContext.getBean( Shell.class );
            shell.setMosaicHome( home );
            return shell;
        }

    }
}
