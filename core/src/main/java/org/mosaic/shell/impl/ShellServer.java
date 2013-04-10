package org.mosaic.shell.impl;

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
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.apache.sshd.server.session.SessionFactory;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.shell.impl.auth.KeyPairProvider;
import org.mosaic.shell.impl.auth.PasswordAuthenticator;
import org.mosaic.shell.impl.auth.PublicKeyAuthenticator;
import org.mosaic.shell.impl.auth.UserAuthLocalhostNone;
import org.mosaic.shell.impl.command.CommandManager;
import org.mosaic.shell.impl.session.MosaicCommand;
import org.mosaic.shell.impl.session.MosaicServerSession;
import org.mosaic.shell.impl.session.MosaicSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Bean
public class ShellServer
{
    private static final Logger LOG = LoggerFactory.getLogger( ShellServer.class );

    private static class MosaicSessionFactory extends SessionFactory
    {
        @Override
        protected AbstractSession doCreateSession( IoSession ioSession ) throws Exception
        {
            return new MosaicServerSession( this.server, ioSession );
        }
    }

    private ModuleManager moduleManager;

    private PasswordAuthenticator passwordAuthenticator;

    private PublicKeyAuthenticator publicKeyAuthenticator;

    private SshServer sshServer;

    private CommandManager shellCommandsManager;

    private KeyPairProvider keyPairProvider;

    @BeanRef
    public void setKeyPairProvider( KeyPairProvider keyPairProvider )
    {
        this.keyPairProvider = keyPairProvider;
    }

    @ServiceRef
    public void setModuleManager( ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @BeanRef
    public void setPasswordAuthenticator( PasswordAuthenticator passwordAuthenticator )
    {
        this.passwordAuthenticator = passwordAuthenticator;
    }

    @BeanRef
    public void setPublicKeyAuthenticator( PublicKeyAuthenticator publicKeyAuthenticator )
    {
        this.publicKeyAuthenticator = publicKeyAuthenticator;
    }

    @BeanRef
    public void setCommandManager( CommandManager shellCommandsManager )
    {
        this.shellCommandsManager = shellCommandsManager;
    }

    @PostConstruct
    public void init() throws IOException
    {
        try
        {
            this.sshServer = SshServer.setUpDefaultServer();
            this.sshServer.setPort( Integer.parseInt( System.getProperty( "sshPort", "7553" ) ) );
            this.sshServer.setShellFactory( new MsoaicShellFactory() );
            this.sshServer.setPasswordAuthenticator( this.passwordAuthenticator );
            this.sshServer.setUserAuthFactories( createUserAuthFactories() );
            this.sshServer.setKeyPairProvider( this.keyPairProvider );
            this.sshServer.setPublickeyAuthenticator( this.publicKeyAuthenticator );
            this.sshServer.setSessionFactory( new MosaicSessionFactory() );
            this.sshServer.setCommandFactory( new CommandFactory()
            {
                @Override
                public Command createCommand( String command )
                {
                    return new MosaicCommand( shellCommandsManager, command );
                }
            } );
            this.sshServer.start();

            // log that we are now started - do not change the message because IntelliJ plugin waits for this to connect!
            LOG.info( "Started SSH server on port {}", this.sshServer.getPort() );
        }
        catch( Exception e )
        {
            LOG.error( "Could not start SSH server: {}", e.getMessage(), e );
        }
    }

    @PreDestroy
    public void destroy()
    {
        // stop the SSH daemon server
        if( this.sshServer != null )
        {
            try
            {
                sshServer.stop( false );

                // log that we are now started - do not change the message because IntelliJ plugin waits for this to connect!
                LOG.info( "Stopped SSH server on port {}", this.sshServer.getPort() );
            }
            catch( Exception e )
            {
                LOG.warn( "Could not stop SSH server bound to {}:{} - {}",
                          sshServer.getHost(),
                          sshServer.getPort(),
                          e.getMessage(),
                          e );
            }
            this.sshServer = null;
        }
    }

    private List<NamedFactory<UserAuth>> createUserAuthFactories()
    {
        List<NamedFactory<UserAuth>> factories = new ArrayList<>();
        factories.add( new UserAuthLocalhostNone.Factory() );
        factories.add( new UserAuthPassword.Factory() );
        factories.add( new UserAuthPublicKey.Factory() );
        return factories;
    }

    private class MsoaicShellFactory implements Factory<Command>
    {
        @Override
        public Command create()
        {
            return new MosaicSession( moduleManager, shellCommandsManager );
        }
    }
}
