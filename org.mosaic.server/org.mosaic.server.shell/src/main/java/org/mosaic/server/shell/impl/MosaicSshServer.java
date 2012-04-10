package org.mosaic.server.shell.impl;

import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.PreDestroy;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.mosaic.MosaicHome;
import org.mosaic.config.ConfigListener;
import org.mosaic.config.Configuration;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class MosaicSshServer {

    private static final Logger LOG = LoggerFactory.getBundleLogger( MosaicSshServer.class );

    private MosaicHome home;

    private MosaicSshShellFactory shellFactory;

    private SshServer sshServer;

    @Autowired
    public void setShellFactory( MosaicSshShellFactory shellFactory ) {
        this.shellFactory = shellFactory;
    }

    @ServiceRef
    public void setHome( MosaicHome home ) {
        this.home = home;
    }

    @ConfigListener( "ssh" )
    public void configure( Configuration cfg ) {

        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort( cfg.getAs( "port", Integer.class, 9080 ) );

        Path hostKeyFile = this.home.getWork().resolve( "host.ser" );
        this.sshServer.setKeyPairProvider( new SimpleGeneratorHostKeyProvider( hostKeyFile.toString() ) );
        this.sshServer.setShellFactory( this.shellFactory );
        this.sshServer.setPasswordAuthenticator( new PasswordAuthenticator() {
            @Override
            public boolean authenticate( String username, String password, ServerSession session ) {
                return password.equals( username );
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
            this.sshServer.stop();
        }
    }

}
