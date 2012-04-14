package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PreDestroy;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.MosaicHome;
import org.mosaic.config.ConfigListener;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
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

    private SshServer sshServer;

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext( ApplicationContext applicationContext ) {
        this.applicationContext = applicationContext;
    }

    @ServiceRef
    public void setHome( MosaicHome home ) {
        this.home = home;
    }

    @ConfigListener( "ssh" )
    public void configure( Configuration cfg ) throws InterruptedException {
        stop();

        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort( cfg.getAs( "port", Integer.class, 9080 ) );
        this.sshServer.setShellFactory( new MosaicSshShellFactory() );
        this.sshServer.setPasswordAuthenticator( new MosaicPasswordAuthenticator() );
        this.sshServer.setUserAuthFactories( createUserAuthFactories() );
        this.sshServer.setKeyPairProvider( new SimpleGeneratorHostKeyProvider( this.home.getWork().resolve( "host.ser" ).toString() ) );

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
        factories.add( new UserAuthNone.Factory() );
        //TODO: factories.add( new UserAuthPassword.Factory() );
        //TODO: factories.add( new UserAuthPublicKey.Factory() );
        return factories;
    }

    private static class MosaicPasswordAuthenticator implements PasswordAuthenticator {

        @Override
        public boolean authenticate( String username, String password, ServerSession session ) {
            return password.equals( username );
        }
    }

    private class MosaicSshShellFactory implements Factory<Command> {

        @Override
        public Command create() {
            return applicationContext.getBean( Shell.class );
        }

    }
}
