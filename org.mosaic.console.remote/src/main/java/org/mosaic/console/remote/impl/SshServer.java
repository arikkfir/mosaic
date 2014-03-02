package org.mosaic.console.remote.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.mosaic.config.Configurable;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.security.Subject;
import org.mosaic.server.Server;
import org.mosaic.util.collections.MapEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Component
final class SshServer
{
    private static final Logger LOG = LoggerFactory.getLogger( SshServer.class );

    static final Session.AttributeKey<Subject> SUBJECT_KEY = new Session.AttributeKey<>();

    @Nullable
    private org.apache.sshd.SshServer sshServer;

    @Nonnull
    @Service
    private Server server;

    @Configurable("shell")
    void configure( @Nonnull final MapEx<String, String> cfg )
    {
        LOG.info( "SSH server configured - {}", this.sshServer != null ? "restarting" : "starting" );
        startSshServer( cfg );
    }

    @PreDestroy
    void destroy()
    {
        // stop the SSH daemon server
        final org.apache.sshd.SshServer sshServer = this.sshServer;
        if( sshServer != null )
        {
            LOG.info( "SSH server module is deactivating" );
            stopSshServer( sshServer );
        }
    }

    private synchronized void startSshServer( @Nonnull MapEx<String, String> cfg )
    {
        org.apache.sshd.SshServer sshServer = SshServer.this.sshServer;

        // stop the SSH daemon server
        if( sshServer != null )
        {
            stopSshServer( sshServer );
        }

        Path etcDir = SshServer.this.server.getEtcPath();
        Path hostKeyFile = etcDir.resolve( "ssh" ).resolve( "host.key" );

        // create and start the SSH daemon server
        sshServer = null;
        try
        {
            sshServer = org.apache.sshd.SshServer.setUpDefaultServer();
            sshServer.setPort( cfg.find( "port", Integer.class ).or( 7553 ) );
            sshServer.setKeyPairProvider( new SimpleGeneratorHostKeyProvider( hostKeyFile.toString() ) );
            sshServer.setPasswordAuthenticator( new PasswordAuthenticatorImpl() );
            sshServer.setPublickeyAuthenticator( new PublickeyAuthenticatorImpl() );
            sshServer.setUserAuthFactories( createUserAuthFactories() );
            sshServer.setShellFactory( new SessionCommand.Factory() );
            sshServer.setCommandFactory( new SingleCommand.Factory() );
            sshServer.start();

            SshServer.this.sshServer = sshServer;
            LOG.info( "Started SSH server ({}:{})", Objects.toString( sshServer.getHost(), "*" ), sshServer.getPort() );
        }
        catch( Throwable e )
        {
            LOG.error( "Could not start SSH server: {}", e.getMessage(), e );
            if( sshServer != null )
            {
                stopSshServer( sshServer );
            }
        }
    }

    private synchronized void stopSshServer( @Nonnull org.apache.sshd.SshServer sshServer )
    {
        try
        {
            sshServer.stop( false );
            this.sshServer = null;
            LOG.info( "Stopped SSH server bound to {}:{}", Objects.toString( sshServer.getHost(), "*" ), sshServer.getPort() );
        }
        catch( Throwable e )
        {
            LOG.warn( "Could not stop SSH server bound to {}:{} - {}",
                      sshServer.getHost(),
                      sshServer.getPort(),
                      e.getMessage(),
                      e );
        }
    }

    @Nonnull
    private List<NamedFactory<UserAuth>> createUserAuthFactories()
    {
        List<NamedFactory<UserAuth>> factories = new ArrayList<>();
        factories.add( new UserAuthLocalhostNone.Factory() );
        factories.add( new UserAuthPublicKey.Factory() );
        factories.add( new UserAuthPassword.Factory() );
        return factories;
    }
}
